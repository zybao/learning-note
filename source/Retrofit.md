http://www.jianshu.com/p/45cb536be2f4

```java
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            // java8默认方法，1.8的新特性
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            // 这里是核心代码了
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.callAdapter.adapt(okHttpCall);
          }
        });
  }
```

```java
  ServiceMethod<?, ?> loadServiceMethod(Method method) {
    // 从缓存里面取出，如果有的话，直接返回好了
    ServiceMethod<?, ?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        // 为null的话，解析方法的注解和返回类型、参数的注解he参数类型，新建一个ServiceMethod
        result = new ServiceMethod.Builder<>(this, method).build();
        // 新建的ServiceMethod加到缓存列表里面
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
```

ServiceMethod.class
```java
    public ServiceMethod build() {
      // 获取CallAdapter的实现，一般为ExecutorCallAdapterFactory.get实现
      callAdapter = createCallAdapter();
      responseType = callAdapter.responseType();
      if (responseType == Response.class || responseType == okhttp3.Response.class) {
        throw methodError("'"
            + Utils.getRawType(responseType).getName()
            + "' is not a valid response body type. Did you mean ResponseBody?");
      }
      // 响应的转换工厂，如GsonConverterFactory
      responseConverter = createResponseConverter();

      for (Annotation annotation : methodAnnotations) {
        // 真正解析方法注解的地方来了
        parseMethodAnnotation(annotation);
      }

      if (httpMethod == null) {
        throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
      }

      if (!hasBody) { // POST方法需要有body或者表单
        if (isMultipart) {
          throw methodError(
              "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
        }
        if (isFormEncoded) {
          throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
              + "request body (e.g., @POST).");
        }
      }

      // 上面是请求方法，下面是请求参数
      int parameterCount = parameterAnnotationsArray.length;
      // ParameterHandler的实现类有很多，包括了各种参数，@Field、@Query等
      parameterHandlers = new ParameterHandler<?>[parameterCount];
      for (int p = 0; p < parameterCount; p++) {
        Type parameterType = parameterTypes[p];
        // 和之前一样的泛型、通配符检查
        if (Utils.hasUnresolvableType(parameterType)) {
          throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
              parameterType);
        }

        // 参数的注解集合
        Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
        if (parameterAnnotations == null) {
          throw parameterError(p, "No Retrofit annotation found.");
        }

        // 生成了对应的参数注解ParameterHandler实例
        parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
      }

      // 对方法的一些检测
      if (relativeUrl == null && !gotUrl) {
        throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
      }
      if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
        throw methodError("Non-body HTTP method cannot contain @Body.");
      }
      if (isFormEncoded && !gotField) {
        throw methodError("Form-encoded method must contain at least one @Field.");
      }
      if (isMultipart && !gotPart) {
        throw methodError("Multipart method must contain at least one @Part.");
      }

      return new ServiceMethod<>(this);
    }
```

# 注解解析
```java
    private void parseMethodAnnotation(Annotation annotation) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
        if (!Void.class.equals(responseType)) {
          throw methodError("HEAD method must use Void as response type.");
        }
      } else if (annotation instanceof PATCH) {
        parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
      } else if (annotation instanceof POST) {
        parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
      } else if (annotation instanceof PUT) {
        parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
      } else if (annotation instanceof OPTIONS) {
        parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
      } else if (annotation instanceof HTTP) {
        HTTP http = (HTTP) annotation;
        parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
      } else if (annotation instanceof retrofit2.http.Headers) {
        String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
        if (headersToParse.length == 0) {
          throw methodError("@Headers annotation is empty.");
        }
        headers = parseHeaders(headersToParse);
      } else if (annotation instanceof Multipart) {
        if (isFormEncoded) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isMultipart = true;
      } else if (annotation instanceof FormUrlEncoded) {
        if (isMultipart) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isFormEncoded = true;
      }
    }

    private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
      if (this.httpMethod != null) {
        throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
            this.httpMethod, httpMethod);
      }
      this.httpMethod = httpMethod;
      this.hasBody = hasBody;

      if (value.isEmpty()) {
        return;
      }

      // Get the relative URL path and existing query string, if present.
      int question = value.indexOf('?');
      if (question != -1 && question < value.length() - 1) {
        // Ensure the query string does not have any named parameters.
        String queryParams = value.substring(question + 1);
        Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
        if (queryParamMatcher.find()) {
          throw methodError("URL query string \"%s\" must not have replace block. "
              + "For dynamic query parameters use @Query.", queryParams);
        }
      }

      this.relativeUrl = value;
      this.relativeUrlParamNames = parsePathParameters(value);
    }
```


Retrofit.Builder
```java
  public Builder client(OkHttpClient client);
  public Builder callFactory(okhttp3.Call.Factory factory);
  public Builder baseUrl(String baseUrl);
  public Builder baseUrl(HttpUrl baseUrl);
  public Builder addConverterFactory(Converter.Factory factory);
  public Builder addConverterFactory(Converter.Factory factory);
  public Builder callbackExecutor(Executor executor);
  public Builder validateEagerly(boolean validateEagerly);

  Builder(Retrofit retrofit) {
    platform = Platform.get();
    callFactory = retrofit.callFactory;
    baseUrl = retrofit.baseUrl;
    converterFactories.addAll(retrofit.converterFactories);
    adapterFactories.addAll(retrofit.adapterFactories);
    // Remove the default, platform-aware call adapter added by build().
    adapterFactories.remove(adapterFactories.size() - 1);
    callbackExecutor = retrofit.callbackExecutor;
    validateEagerly = retrofit.validateEagerly;
  }

  public Retrofit build() {
    if (baseUrl == null) {
      throw new IllegalStateException("Base URL required.");
    }

    okhttp3.Call.Factory callFactory = this.callFactory;
    if (callFactory == null) {
      callFactory = new OkHttpClient();
    }

    Executor callbackExecutor = this.callbackExecutor;
    if (callbackExecutor == null) {
      callbackExecutor = platform.defaultCallbackExecutor();
    }

    // Make a defensive copy of the adapters and add the default Call adapter.
    List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
    adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

    // Make a defensive copy of the converters.
    List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

    return new Retrofit(callFactory, baseUrl, converterFactories, adapterFactories,
      callbackExecutor, validateEagerly);
  }
```



# Platform
```java
class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
      if (Build.VERSION.SDK_INT != 0) {
        return new Android();
      }
    } catch (ClassNotFoundException ignored) {
    }
    try {
      Class.forName("java.util.Optional");
      return new Java8();
    } catch (ClassNotFoundException ignored) {
    }
    return new Platform();
  }

  @Nullable Executor defaultCallbackExecutor() {
    return null;
  }

  CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
    if (callbackExecutor != null) {
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }
    return DefaultCallAdapterFactory.INSTANCE;
  }

  boolean isDefaultMethod(Method method) {
    return false;
  }

  @Nullable Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
      @Nullable Object... args) throws Throwable {
    throw new UnsupportedOperationException();
  }
}
```
