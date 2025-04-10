package com.ray.opengl.filter.ar


import android.content.Context
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.os.Handler
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.Toast
import com.google.android.filament.Fence
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.KTXLoader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.zeusee.main.hyperlandmark.jni.Face
import java.nio.ByteBuffer

class HelmetFilter {
    companion object {
        // Load the library for the utility layer, which in turn loads gltfio and the Filament core.
        init {
            Utils.init()
        }

        private const val TAG = "gltf-viewer"
    }

    lateinit var face: Face
    private lateinit var choreographer: Choreographer
    private val frameScheduler = FrameCallback()
    private lateinit var modelViewer: ModelViewer
    private var statusToast: Toast? = null
    private var statusText: String? = null
    private var latestDownload: String? = null
    private val automation = AutomationEngine()
    private var loadStartTime = 0L
    private var loadStartFence: Fence? = null
    private val viewerContent = AutomationEngine.ViewerContent()
    private var surfaceViewOverlap: SurfaceView;
    private var mModelMatrix = FloatArray(16)
    private var context: Context

    var PREVIEW_HEIGHT: Int = 640;
    var PREVIEW_WIDTH: Int = 480;
    private var mWidth = 0
    private var mHeight = 0

    constructor(context: Context, surfaceViewOverlap: SurfaceView) {
        this.context = context;
        this.surfaceViewOverlap = surfaceViewOverlap;

        Handler().post {
            Log.v("constructorDD",Thread.currentThread().name)
//        Handler(Looper.getMainLooper()).post {
            choreographer = Choreographer.getInstance()
//        }

            modelViewer = ModelViewer(surfaceViewOverlap)
            viewerContent.view = modelViewer.view
            viewerContent.sunlight = modelViewer.light
            viewerContent.lightManager = modelViewer.engine.lightManager
            viewerContent.scene = modelViewer.scene
            viewerContent.renderer = modelViewer.renderer

            surfaceViewOverlap.setOnTouchListener { _, event ->
                modelViewer.onTouchEvent(event)
//            doubleTapDetector.onTouchEvent(event)
                true
            }

            createDefaultRenderables()
            createIndirectLight()
            val view = modelViewer.view
            view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
                enabled = true
                quality = View.QualityLevel.MEDIUM
            }

            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                enabled = true
                power = 0.1f
                resolution = 0.1f

            }

            view.bloomOptions = view.bloomOptions.apply {
                enabled = true
            }
            modelViewer.animator.apply { }
            view.blendMode = View.BlendMode.TRANSLUCENT
            modelViewer.transformToUnitCube()

        }

    }

    inner class FrameCallback : Choreographer.FrameCallback {

        private val startTime = System.nanoTime()
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)

            loadStartFence?.let {
                if (it.wait(Fence.Mode.FLUSH, 0) == Fence.FenceStatus.CONDITION_SATISFIED) {
                    val end = System.nanoTime()
                    val total = (end - loadStartTime) / 1_000_000
                    Log.i(TAG, "The Filament backend took $total ms to load the model geometry.")
                    modelViewer.engine.destroyFence(it)
                    loadStartFence = null
                }
            }

            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    val count: Int = animationCount;
                    var nameArray = Array(count) { i -> getAnimationName(i) }

                    val elapsedTimeSeconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
                    applyAnimation(0, elapsedTimeSeconds.toFloat())
                }
//                applyAnimation(0, 1.25f)
                updateBoneMatrices()
            }
            modelViewer.asset?.apply {
                val a = getRoot()
//                glSurfaceView.setModelRootIndex(a)
                val b = popRenderable()
                Log.v("filament_render", "frameTimeNanos$a+$b")
//                val c = popRenderables(null)
                val d = entities
                val e = lightEntities
                val f = cameraEntities
                val g = getFirstEntityByName("")
                val h = getEntitiesByName("")
                val i = getEntitiesByPrefix("")
                val j = materialInstances
                val k = boundingBox
                val l = getName(42)
                val m = getExtras(0)
//                val n = getAnimator()
                val o = resourceUris
            }
            Log.v("filament_render", "frameTimeNanos$frameTimeNanos")
            modelViewer.render(frameTimeNanos)
            surfaceViewOverlap.setZOrderOnTop(true)
            surfaceViewOverlap.holder.setFormat(PixelFormat.TRANSLUCENT)

//            modelViewer.view.viewport = modelViewer.view.viewport.apply {
//                left = 100
//                bottom = 100
//                width = 100
//                height = 100
//            }
//            modelViewer.renderer.
        }
    }


    private fun createDefaultRenderables() {
//        val buffer = assets.open("models/primitive-animals.gltf").use { input ->
        val buffer = context.assets.open("models/DamagedHelmet.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }

        modelViewer.loadModelGltfAsync(buffer) { uri -> readCompressedAsset("models/$uri") }
        updateRootTransform()
    }

    private fun createIndirectLight() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val ibl = "default_env"
        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
            scene.indirectLight = KTXLoader.createIndirectLight(engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }
//        modelViewer.engine.renderableManager
//        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
//            scene.skybox = KTXLoader.createSkybox(engine, it)
//        }
    }


    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = context.assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    fun onDrawFrame() {
        val left: Int = face.left * mWidth / PREVIEW_HEIGHT
        //这里bottom为靠近底部的距离，而不是坐标.
        val bottom: Int = (PREVIEW_WIDTH - face.bottom) * mHeight / PREVIEW_WIDTH
        val faceWidth: Int = face.width * mWidth / PREVIEW_HEIGHT
        val faceHeight: Int = face.height * mHeight / PREVIEW_WIDTH
        Log.e("viewport:face111", mWidth.toString() + "  " + mHeight)
        Log.e(
            "viewport:face222",
            "$left  $bottom  $faceWidth   $faceHeight"
        )
        val viewport = Viewport(left - 200, bottom - 500, 2 * faceWidth, 3 * faceHeight)
        modelViewer.view.viewport = viewport
//        if (modelRootIndex != 0) {
//                        MatrixOperation.eulerAnglesToRotationMatrix(face.eulerAngles);
//                        Mat3 transform =  MatrixOperation.eulerAnglesToRotationMatrix(face.eulerAngles);
//                        Log.v("isoen transform:", transform.toFloatArray().toString());
//                        modelViewer.getEngine().getTransformManager().setTransform(modelRootIndex,transform.toFloatArray());


//                        float scaleRatio = mWidth / mHeight;
            val scaleRatio = 0.75f
            var pitchAngle = face.eulerAngles[0]
            var yawAngle = face.eulerAngles[1]
            val rollAngle = face.eulerAngles[2]
            Log.v(
                "欧拉角:",
                "pitchAngle:" + face.eulerAngles[0] + "yawAngle:" + face.eulerAngles[1] + "rollAngle:" + face.eulerAngles[2]
            )
            // 限定左右扭头幅度不超过50°，销毁人脸关键点SDK带来的偏差
            if (Math.abs(yawAngle) > 50) {
                yawAngle = yawAngle / Math.abs(yawAngle) * 50
            }
            // 限定抬头低头最大角度，消除人脸关键点SDK带来的偏差
            if (Math.abs(pitchAngle) > 30) {
                pitchAngle = pitchAngle / Math.abs(pitchAngle) * 30
            }
            Log.v(
                "欧拉角02:",
                "pitchAngle:" + pitchAngle + "yawAngle:" + yawAngle + "rollAngle:" + rollAngle
            )
            //将模型举证设置为单位举证
            Matrix.setIdentityM(mModelMatrix, 0)
            Matrix.translateM(mModelMatrix, 0, 0f, 0f, 1f)
            Matrix.scaleM(mModelMatrix, 0, scaleRatio, scaleRatio, scaleRatio)
            Matrix.rotateM(mModelMatrix, 0, -rollAngle, 0f, 0f, 1f)
            Matrix.rotateM(mModelMatrix, 0, -yawAngle, 0f, 1f, 0f)
            Matrix.rotateM(mModelMatrix, 0, 90f, 1f, 0f, 0f)
            Matrix.rotateM(mModelMatrix, 0, -pitchAngle, 1f, 0f, 0f)
            modelViewer.engine.transformManager.setTransform(modelViewer.asset!!.root, mModelMatrix)

            choreographer.postFrameCallback(frameScheduler)
//        }
    }

    fun onReady(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    fun release() {
        val viewport = Viewport(0, 0, 0, 0)
        modelViewer.view.viewport = viewport
        modelViewer.renderer.clearOptions.clear = true
        modelViewer.clearRootTransform();
    }
}