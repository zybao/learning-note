# [ConstraintLayout](https://developer.android.com/training/constraint-layout/index.html)

http://blog.coderclock.com/2017/04/09/android/android-constraintlayout/

ConstraintLayout总共有下面这些同类的属性:
* layout_constraintLeft_toLeftOf
* layout_constraintLeft_toRightOf
* layout_constraintRight_toLeftOf
* layout_constraintRight_toRightOf
* layout_constraintTop_toTopOf
* layout_constraintTop_toBottomOf
* layout_constraintBottom_toTopOf
* layout_constraintBottom_toBottomOf
* layout_constraintBaseline_toBaselineOf
* layout_constraintStart_toEndOf
* layout_constraintStart_toStartOf
* layout_constraintEnd_toStartOf
* layout_constraintEnd_toEndOf

下面介绍一些Margin属性:
* android:layout_marginStart
* android:layout_marginEnd
* android:layout_marginLeft
* android:layout_marginTop
* android:layout_marginRight
* android:layout_marginBottom

当约束对象是GONE的时候，可以使用以下属性：
* layout_goneMarginStart
* layout_goneMarginEnd
* layout_goneMarginLeft
* layout_goneMarginTop
* layout_goneMarginRight
* layout_goneMarginBottom

另外，ConstraintLayout新增了如下两个属性用于控制控件在水平和垂直方向在屏幕上的偏移比例：
* layout_constraintHorizontal_bias
* layout_constraintVertical_bias

# Guideline
如果我们需要对着屏幕的中轴线进行布局，就可以使用到Guideline进行操作