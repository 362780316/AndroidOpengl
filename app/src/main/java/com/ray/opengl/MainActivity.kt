package com.ray.opengl

import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.Fence
import com.google.android.filament.View
import com.google.android.filament.utils.*
import com.ray.opengl.MyRecordButton.OnRecordListener
import com.ray.opengl.makeup.bean.DynamicMakeup
import com.ray.opengl.resource.MakeupHelper
import com.ray.opengl.resource.ResourceHelper
import com.ray.opengl.resource.ResourceJsonCodec
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    companion object {
        init {
            Utils.init()
        }

        private const val TAG = "gltf-viewer"
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        initResources()
        val point = Point()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getSize(point)
        val screenWidth = point.x
        val screenHeight = point.y
        println("MainActivity手机的分辨率:$screenWidth~~~~$screenHeight")

        initUi();
        initGltf()
    }

    private fun initGltf() {
        choreographer = Choreographer.getInstance()
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
        view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }

        view.ambientOcclusionOptions.apply {
            enabled = true
            power = 0.1f
            resolution = 0.1f
        }

        view.bloomOptions.apply {
            enabled = true
        }
        view.blendMode = View.BlendMode.TRANSLUCENT
        modelViewer.transformToUnitCube()
    }

    private fun initUi() {
        chk_beauty02.isChecked = true
        glSurfaceView.post { glSurfaceView.enableBeauty02(true) }
        chk_beauty02.setOnCheckedChangeListener { _, isChecked ->
            glSurfaceView.enableBeauty02(isChecked)
        }
        chk_cat.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableCatstick(isChecked, null)
        }
        chk_helmet.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                choreographer.postFrameCallback(frameScheduler)
            } else {
                choreographer.removeFrameCallback(frameScheduler)
            }
            glSurfaceView.enableHelmet(isChecked)
            surfaceViewOverlap.visibility = if (isChecked) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

        btn_record.setOnRecordListener(
            object : OnRecordListener {
                override fun onStartRecording() {
                    glSurfaceView.startRecording()
                }

                override fun onStopRecording() {
                    glSurfaceView.stopRecording()
                    Toast.makeText(this@MainActivity, "录制完成！", Toast.LENGTH_SHORT).show()
                }
            })
        group_record_speed.setOnCheckedChangeListener { _, checkedId ->
            /**
             * 选择录制模式
             * @param group
             * @param checkedId
             */
            when (checkedId) {
                R.id.rbtn_record_speed_extra_slow -> glSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_SLOW)
                R.id.rbtn_record_speed_slow -> glSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_SLOW)
                R.id.rbtn_record_speed_normal -> glSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_NORMAL)
                R.id.rbtn_record_speed_fast -> glSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_FAST)
                R.id.rbtn_record_speed_extra_fast -> glSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_FAST)
            }
        }
        chk_bigeye.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableBigEye(
                isChecked
            )
        }
        // TODO 下面是 NDK OpenGL 新增点
        chk_stick.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableStick(
                isChecked
            )
        }
        chk_beauty.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableBeauty(
                isChecked
            )
        }
        chk_demo.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableDemo(
                isChecked
            )
        }
        chk_thinface.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableThinFace(
                isChecked
            )
        }
        chk_brighteye.setOnCheckedChangeListener { _, isChecked: Boolean ->
            glSurfaceView.enableBrightEye(
                isChecked
            )
        }
        chk_lipstick.setOnCheckedChangeListener { _, isChecked ->
            val folderPath = MakeupHelper.getMakeupDirectory(this@MainActivity) + File.separator +
                    MakeupHelper.getMakeupList()[1].unzipFolder
            var makeup: DynamicMakeup? = null
            try {
                makeup = ResourceJsonCodec.decodeMakeupData(folderPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            glSurfaceView.enableLipstick(isChecked, makeup)
        }
    }

    private fun createDefaultRenderables() {
//        val buffer = assets.open("models/primitive-animals.gltf").use { input ->
        val buffer = assets.open("models/DamagedHelmet.gltf").use { input ->
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
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    /**
     * 初始化动态贴纸、滤镜等资源
     */
    private fun initResources() {
        Thread {
            ResourceHelper.initAssetsResource(this@MainActivity)
            //            FilterHelper.initAssetsFilter(MainActivity.this);
            MakeupHelper.initAssetsMakeup(this@MainActivity)
        }.start()
    }

    fun getmOverlap(): SurfaceView? {
        return surfaceViewOverlap
    }

    fun getModelView(): ModelViewer {
        return modelViewer
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
                glSurfaceView.setModelRootIndex(a)
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
//                left = 0
//                bottom = 0
//                width = 200
//                height = 400
//            }
//            modelViewer.renderer.
        }
    }


    override fun onResume() {
        super.onResume()
//        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameScheduler)
    }
}