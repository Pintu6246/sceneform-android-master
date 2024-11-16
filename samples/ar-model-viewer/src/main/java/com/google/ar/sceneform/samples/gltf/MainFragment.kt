package com.google.ar.sceneform.samples.gltf

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene

    private var model: Renderable? = null
    private var modelView: ViewRenderable? = null

    private var isPreviewMode = false // Toggle for AR or screen preview mode

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find and configure the AR fragment
        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
            }
            setOnTapArPlaneListener(::onTapPlane)
        }

        // Set up the mode toggle button
        val toggleButton: Button = view.findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener {
            isPreviewMode = !isPreviewMode
            Toast.makeText(context, if (isPreviewMode) "Preview Mode" else "AR Mode", Toast.LENGTH_SHORT).show()
            if (isPreviewMode) {
                showPreviewModel()
            } else {
                hidePreviewModel()
            }
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
        }
    }

    private suspend fun loadModels() {
        model = ModelRenderable.builder()
            .setSource(context, Uri.parse("models/Bosch1.glb"))
            .setIsFilamentGltf(true)
            .await()
        modelView = ViewRenderable.builder()
            .setView(context, R.layout.view_renderable_infos)
            .await()
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (isPreviewMode) return // Ignore taps in preview mode

        if (model == null || modelView == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
            // Create the transformable model and add it to the anchor.
            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                localScale=Vector3(0.01f, 0.01f, 0.01f)
                localPosition = Vector3(0.0f, 0.0f, -2.0f) // 1 meter ahead in the Z-axis
                renderableInstance.setCulling(false)
                renderableInstance.animate(true).start()
                // Add the View
                addChild(Node().apply {
                    // Define the relative position
                    localPosition = Vector3(0.0f, 0.5f, -2.0f) // Half a meter above the model
                    localScale = Vector3(0.2f, 0.2f, 0.2f)
                    renderable = modelView
                })
            })
        })
    }

    private fun showPreviewModel() {
        if (model == null) {
            Toast.makeText(context, "Loading model...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a Node at the center of the screen
        val previewNode = Node().apply {
            renderable = model
            localScale = Vector3(0.2f, 0.2f, 0.2f) // Adjust scale as needed

            localPosition = Vector3(0.0f, 0.3f, -2.0f) // Position in front of the camera
        }

        scene.addChild(previewNode)

        // Animate the model
        previewNode.renderableInstance.animate(true).start()

        // Store the node in a tag for cleanup later
        arSceneView.tag = previewNode
    }

    private fun hidePreviewModel() {
        // Remove the preview node if it exists
        (arSceneView.tag as? Node)?.let {
            scene.removeChild(it)
        }
        arSceneView.tag = null
    }
}