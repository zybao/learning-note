


# Transformation
versoin 3.7.0
```java
public class MaskTransformation implements Transformation<Bitmap> {

    private int maxWidth;
    private int maxHeight;

    private static Paint sMaskingPaint = new Paint();
    private Context context;
    private BitmapPool bitmapPool;
    private int maskId;
    private float scale = 1f;

    static {
        sMaskingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    private int screenWidth;
    private int screenHeight;

    /**
     * @param maskId If you change the mask file, please also rename the mask file, or Glide will get
     *               the cache with the old mask. Because getId() return the same values if using the
     *               same make file name. If you have a good idea please tell us, thanks.
     */
    public MaskTransformation(Context context, int maskId) {
        this(context, Glide.get(context).getBitmapPool(), maskId);
    }

    public MaskTransformation(Context context, BitmapPool pool, int maskId) {
        this(context, pool, maskId, -1);
    }


    public MaskTransformation(Context context, BitmapPool pool, int maskId, float scale) {
        bitmapPool = pool;
        this.context = context.getApplicationContext();
        this.maskId = maskId;
        this.scale = scale;
        getMobileScale();
        maxWidth = screenWidth * 4 / 10;
        maxHeight = screenHeight * 3 / 10;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        Bitmap source = resource.get();

        int originWidth = source.getWidth();
        int originHeight = source.getHeight();


        int width, height;
        if (originWidth > originHeight) {
            width = originWidth > maxWidth ? maxWidth : originWidth;
            scale = (width + 0f) / originWidth;
            height = (int) (originHeight * scale);
        } else {
            height = originHeight > maxHeight ? maxHeight : originHeight;
            scale = (height + 0f) / originHeight;
            width = (int) (originWidth * scale);
        }


        Bitmap result = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888);
        if (result == null) {
            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        Drawable mask = getMaskDrawable(context, maskId);

        Canvas canvas = new Canvas(result);
        mask.setBounds(0, 0, width, height);
        mask.draw(canvas);
        canvas.drawBitmap(compress(source, scale), 0, 0, sMaskingPaint);

        return BitmapResource.obtain(result, bitmapPool);
    }
    
    
    Bitmap compress(Bitmap source, float scale){
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public String getId() {
        return "MaskTransformation(maskId=" + context.getResources().getResourceEntryName(maskId)
                + ")";
    }

    private Drawable getMaskDrawable(Context context, int maskId) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getDrawable(maskId);
        } else {
            drawable = context.getResources().getDrawable(maskId);
        }

        if (drawable == null) {
            throw new IllegalArgumentException("maskId is invalid");
        }

        return drawable;
    }

    /**
     * @return screenHeight / screenWidth
     */
    private float getMobileScale() {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        return screenHeight / (screenWidth + 0f);
    }
}
```