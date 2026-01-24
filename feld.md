# feld.md — OpenGL ES Touch-Deformed Field (Android)

## Goal
Create a real-time “force / gravitation field” effect on Android where:
- The user’s finger deforms space (a curved field).
- The deformation is computed on the GPU using shader and/or conputebshaders
- A background image (or scene) is warped by this field.
- This is first tested in a standalone **FeldtestActivity**.
- After validation, the same renderer is embedded into a **custom Android Launcher**.
- Icons / UI can later be warped by the same field logic.

---

## High-Level Concept
- Use **OpenGL ES 3.1**
- Render a **full-screen quad**
- Apply a **fragment shader** that:
  - Receives touch positions
    - Computes a force-field distortion
      - Warps texture coordinates
      - Render into a **transparent GLSurfaceView**
      - Overlay it on top of normal Android UI (or under icons)

      ---

      ## Architecture Overview

      FeldtestActivity └── Transparent GLSurfaceView └── FieldRenderer ├── Vertex Shader (pass-through) ├── Fragment Shader (field distortion) └── Touch input → shader uniforms

      ---

      ## Step 1: Feldtest Activity

      ### FeldtestActivity.kt
      ```kotlin
      class FeldtestActivity : Activity() {

          override fun onCreate(savedInstanceState: Bundle?) {
                  super.onCreate(savedInstanceState)

                          val glView = FieldGLSurfaceView(this)
                                  setContentView(glView)
                                      }
                                      }


                                      ---

                                      Step 2: Transparent GLSurfaceView

                                      class FieldGLSurfaceView(context: Context) : GLSurfaceView(context) {

                                          private val renderer: FieldRenderer

                                              init {
                                                      setEGLContextClientVersion(2)

                                                              setEGLConfigChooser(
                                                                          8, 8, 8, 8,  // RGBA
                                                                                      16, 0
                                                                                              )
                                                                                                      holder.setFormat(PixelFormat.TRANSLUCENT)
                                                                                                              setZOrderOnTop(true)

                                                                                                                      renderer = FieldRenderer(context)
                                                                                                                              setRenderer(renderer)

                                                                                                                                      renderMode = RENDERMODE_CONTINUOUSLY
                                                                                                                                          }

                                                                                                                                              override fun onTouchEvent(event: MotionEvent): Boolean {
                                                                                                                                                      renderer.setTouch(
                                                                                                                                                                  event.x / width.toFloat(),
                                                                                                                                                                              1f - event.y / height.toFloat()
                                                                                                                                                                                      )
                                                                                                                                                                                              return true
                                                                                                                                                                                                  }
                                                                                                                                                                                                  }


                                                                                                                                                                                                  ---

                                                                                                                                                                                                  Step 3: Renderer Core

                                                                                                                                                                                                  class FieldRenderer(val context: Context) : GLSurfaceView.Renderer {

                                                                                                                                                                                                      private var touchX = 0.5f
                                                                                                                                                                                                          private var touchY = 0.5f

                                                                                                                                                                                                              fun setTouch(x: Float, y: Float) {
                                                                                                                                                                                                                      touchX = x
                                                                                                                                                                                                                              touchY = y
                                                                                                                                                                                                                                  }

                                                                                                                                                                                                                                      override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                                                                                                                                                                                                                                              GLES20.glClearColor(0f, 0f, 0f, 0f)
                                                                                                                                                                                                                                                      // Load shaders, textures, buffers
                                                                                                                                                                                                                                                          }

                                                                                                                                                                                                                                                              override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
                                                                                                                                                                                                                                                                      GLES20.glViewport(0, 0, w, h)
                                                                                                                                                                                                                                                                          }

                                                                                                                                                                                                                                                                              override fun onDrawFrame(gl: GL10?) {
                                                                                                                                                                                                                                                                                      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

                                                                                                                                                                                                                                                                                              // Pass uniforms
                                                                                                                                                                                                                                                                                                      // uTouchPos = vec2(touchX, touchY)
                                                                                                                                                                                                                                                                                                              // uStrength = float
                                                                                                                                                                                                                                                                                                                      // Draw full-screen quad
                                                                                                                                                                                                                                                                                                                          }
                                                                                                                                                                                                                                                                                                                          }


                                                                                                                                                                                                                                                                                                                          ---

                                                                                                                                                                                                                                                                                                                          Step 4: Vertex Shader (Simple Pass-through)

                                                                                                                                                                                                                                                                                                                          attribute vec2 aPos;
                                                                                                                                                                                                                                                                                                                          varying vec2 vUV;

                                                                                                                                                                                                                                                                                                                          void main() {
                                                                                                                                                                                                                                                                                                                              vUV = aPos * 0.5 + 0.5;
                                                                                                                                                                                                                                                                                                                                  gl_Position = vec4(aPos, 0.0, 1.0);
                                                                                                                                                                                                                                                                                                                                  }


                                                                                                                                                                                                                                                                                                                                  ---

                                                                                                                                                                                                                                                                                                                                  Step 5: Fragment Shader (Force Field Warp)

                                                                                                                                                                                                                                                                                                                                  precision mediump float;

                                                                                                                                                                                                                                                                                                                                  uniform sampler2D uTexture;
                                                                                                                                                                                                                                                                                                                                  uniform vec2 uTouch;
                                                                                                                                                                                                                                                                                                                                  uniform float uStrength;

                                                                                                                                                                                                                                                                                                                                  varying vec2 vUV;

                                                                                                                                                                                                                                                                                                                                  void main() {
                                                                                                                                                                                                                                                                                                                                      vec2 dir = vUV - uTouch;
                                                                                                                                                                                                                                                                                                                                          float dist = length(dir);

                                                                                                                                                                                                                                                                                                                                              float force = uStrength / (dist * dist + 0.01);
                                                                                                                                                                                                                                                                                                                                                  vec2 warpedUV = vUV + normalize(dir) * force;

                                                                                                                                                                                                                                                                                                                                                      gl_FragColor = texture2D(uTexture, warpedUV);
                                                                                                                                                                                                                                                                                                                                                      }

                                                                                                                                                                                                                                                                                                                                                      Result:

                                                                                                                                                                                                                                                                                                                                                      Space bends inward/outward around the finger

                                                                                                                                                                                                                                                                                                                                                      Looks like gravity / force distortion

                                                                                                                                                                                                                                                                                                                                                      Fully GPU-driven, real-time



                                                                                                                                                                                                                                                                                                                                                      ---

                                                                                                                                                                                                                                                                                                                                                      Step 6: Validation Checklist (Feldtest)

                                                                                                                                                                                                                                                                                                                                                      ✔ Smooth 60 FPS

                                                                                                                                                                                                                                                                                                                                                      ✔ No UI lag

                                                                                                                                                                                                                                                                                                                                                      ✔ Touch deformation feels responsive

                                                                                                                                                                                                                                                                                                                                                      ✔ Transparency works over Android UI


                                                                                                                                                                                                                                                                                                                                                      Only after this passes → integrate into launcher.


                                                                                                                                                                                                                                                                                                                                                      ---

                                                                                                                                                                                                                                                                                                                                                      Step 7: Integration into Custom Launcher

                                                                                                                                                                                                                                                                                                                                                      Strategy

                                                                                                                                                                                                                                                                                                                                                      Use same GLSurfaceView

                                                                                                                                                                                                                                                                                                                                                      Place it:

                                                                                                                                                                                                                                                                                                                                                      Below icons (background distortion)

                                                                                                                                                                                                                                                                                                                                                      Or above icons (icon warping)


                                                                                                                                                                                                                                                                                                                                                      Feed icon positions into shader as additional field sources


                                                                                                                                                                                                                                                                                                                                                      Icon Warping (Advanced)

                                                                                                                                                                                                                                                                                                                                                      Render icons into an offscreen texture (FBO)

                                                                                                                                                                                                                                                                                                                                                      Apply same fragment shader

                                                                                                                                                                                                                                                                                                                                                      Output warped icon layer to screen



                                                                                                                                                                                                                                                                                                                                                      ---

                                                                                                                                                                                                                                                                                                                                                      Performance Notes

                                                                                                                                                                                                                                                                                                                                                      All deformation math is in fragment shader

                                                                                                                                                                                                                                                                                                                                                      CPU only sends touch coordinates

                                                                                                                                                                                                                                                                                                                                                      No allocations per frame

                                                                                                                                                                                                                                                                                                                                                      Works on most GPUs (Adreno / Mali)



                                                                                                                                                                                                                                                                                                                                                      ---

                                                                                                                                                                                                                                                                                                                                                      Final Result

                                                                                                                                                                                                                                                                                                                                                      A live, GPU-driven, touch-reactive gravitational field launcher where the user literally bends space with their finger.


                                                                                                                                                                                                                                                                                                                                                      ---
