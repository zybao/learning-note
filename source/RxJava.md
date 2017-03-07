# RxJava

    Observer<Object> observer = new Observer<Object>() {
        @Override
        public void onNext(Object obj) {

        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }
    }

    Subscriber<Object> subscriber = new Subscriber<Object>() {
        @Override
        public void onNext(Object obj) {

        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }
    }

## 创建Observable

        Observable observable = Observablew.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("Hello");
                subscriber.onNext("Hi");
                subscriber.onCompleted();
            }
        });

    Observavle observable = Observable.just("Hello", "Ni Hao");

    String[] words = {'Hello', "Welcome", "To"}
    Observable observable = Observable.from(words);

### Subscribe(订阅)
    
    observable.subscribe(observer);
    observable.subscribe(subscriber);

    Action1<String> onNextAction = new Action1<String>() {
        @Override
        public void call(String s) {

        }
    }

    Action1<Throwable> onErrorAction = new Action1<Throwable>() {
        @Override
        public void call(Throwable e) {

        }
    }

    Action0 onCompletedAction = new Action0() {
        @Override
        public void call() {

        }
    }

    observable.subscribe(onNextAction);
    observable.subscribe(onNextAction, onErrorAction, onCompletedAction);

### 线程控制
Schedulers.immediate();
Schedulers.newThread();
Schedulers.io();
Schedulers.computation();
AndriodSchedulers.mainThread();

### 变换
    Observable.just('images/logo.png')
        .map(new Func1<String, Bitmap>() {
            @Override
            public Bitmap call(String filePath) {
                return getBitmapFromPath(filePath);
            }
        })
        .subscribe(new Action1<Bitmap> () {
            @Override
            public void call(Bitmap bitmap) {
                showBitmap(bitmap);
            }
        });

#### flatMap

    Student[] students = ...;
    Subscriber<Course> subscriber = new Subscriber<Course>() {
        @Override
        public void onNext(Course course) {

        }
    };

    Observable.from(students)
        .flatMap(new Func1<Student, Observable<Course>> () {
            @Override
            public Observable<Course> call(Student student) {
                return Observable.from(student.getCourses());
            }
        })
        .subscribe(subscriber);

    重点按钮：

    RxView.clickEvents(button)
        .throttleFirst(500, TimeUnit.MILLISECONDS)
        .subscribe(subscriber);

#### lift()

    public <R> Observable<R> lift(Operator<? extends R, ? super T> operator) {
        return Observable.create(new Onsubscriber<R>() {
            @Override
            public void call(Subscriber subscriber) {
                Subscriber newSubscriber = operator.call(subscriber);
                newSubscriber.onStart();
                onSubscribe.call(newSubscriber);
            }
        })
    }

    observable.lift(new Observable.Operator<String, Integer>() {
        @Override
        public Subscriber<? super Integer> call(final Subscriber<? super String> subscriber) {
            return new Subscriber<Integer>() {
                @Override
                public vois onNext(Integer integer) {
                    subscriber.onNext("" + integer);
                }

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }
            }
        }
    })

#### compose()

    public class LiftAllTransformer implements Observable.Transformer<Integer, String> {
        @Override
        public Observable<String> call(Observable<Integer> observable) {
            return observable
                .lift1()
                .lift2()
                .lift3()
                .lift4();
        }
    }

    Transformer liftAll = new LiftAllTransformer();
    observable1.compose(liftAll).subscribe(subscriber1);
    observable2.compose(liftAll).subscribe(subscriber2);
    observable3.compose(liftAll).subscribe(subscriber3);
    observable4.compose(liftAll).subscribe(subscriber4);

### RxBinding

    private PublishSubject<AlertDialog> alertDialog = PublishSubject.create();
    Observable<TextView> cancelButton = alertDialog
        .map(a -> ButterKnife.findById(a, R.id.cancel_button));

    cancelButton.switchMap(RxView::clicks)
        .observeOn(AndroidSchedulers.mainThread())
        .compose(bind)
        .subscribe(new Action1<Object>() {
            @Override
            public void call(Object obj) {
                # do something
            }
        });
### 分类


    Creating Observables(Observable的创建操作符)，比如：Observable.create()、Observable.just()、Observable.from()等等；
    
    Transforming Observables(Observable的转换操作符)，比如：observable.map()、observable.flatMap()、observable.buffer()等等；
    
    Filtering Observables(Observable的过滤操作符)，比如：observable.filter()、observable.sample()、observable.take()等等；
    
    Combining Observables(Observable的组合操作符)，比如：observable.join()、observable.merge()、observable.combineLatest()等等；
    
    Error Handling Operators(Observable的错误处理操作符)，比如:observable.onErrorResumeNext()、observable.retry()等等；
    
    Observable Utility Operators(Observable的功能性操作符)，比如：observable.subscribeOn()、observable.observeOn()、observable.delay()等等；
    
    Conditional and Boolean Operators(Observable的条件操作符)，比如：observable.amb()、observable.contains()、observable.skipUntil()等等；

    Mathematical and Aggregate Operators(Observable数学运算及聚合操作符)，比如：observable.count()、observable.reduce()、observable.concat()等等；

    其他如observable.toList()、observable.connect()、observable.publish()等等；

### 错误处理操作符
#### Catch

RxJava将Catch实现为三个不同的操作符：

onErrorReturn：让Observable遇到错误时发射一个特殊的项并且正常终止。

onErrorResumeNext：让Observable在遇到错误时开始发射第二个Observable的数据序列。

onExceptionResumeNext：让Observable在遇到错误时继续发射后面的数据项。

