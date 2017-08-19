## Step 1: init tangram
```java
    TangramBuilder.init(this, new IInnerImageSetter() {
        @Override
        public <IMAGE extends ImageView> void doLoadImageUrl(@NonNull IMAGE view,
                    @Nullable String url) {
            Picasso.with(TangramActivity.this).load(url).into(view);
        }
    }, ImageView.class);
```

## Step 2: register build=in cells and cards
```java
    builder = TangramBuilder.newInnerBuilder(this);

    /**
     * init a {@link TangramEngine} builder with build-in resource inited, such as registering build-in card and cell. Users use this builder to regiser custom card and cell, then call {@link InnerBuilder#build()} to create a {@link TangramEngine} instance.
     * @param context activity context
     * @return a {@link TangramEngine} builder
     */
    @NonNull
    public static InnerBuilder newInnerBuilder(@NonNull final Context context) {
        if (!TangramBuilder.isInitialized()) {
            throw new IllegalStateException("Tangram must be init first");
        }

        DefaultResolverRegistry registry = new DefaultResolverRegistry();

        // install default cards & mCells
        installDefaultRegistry(registry);

        return new InnerBuilder(context, registry);
    }
```