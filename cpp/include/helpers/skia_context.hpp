#include <jni.h>
#include <mutex>
#include <memory>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>

#include "thread.hpp" // Threading annotations.
#include "GrDirectContext.h"
#include "SkColorSpace.h"

namespace rive_android
{
class SkiaContextManager
{
public:
    // Singleton getter.
    static std::shared_ptr<SkiaContextManager> getInstance();

    // Singleton can't be copied/assigned/moved.
    SkiaContextManager(SkiaContextManager const&) = delete;
    SkiaContextManager& operator=(SkiaContextManager const&) = delete;
    SkiaContextManager(SkiaContextManager&&) = delete;
    SkiaContextManager& operator=(SkiaContextManager&&) = delete;

    std::mutex eglCtxMutex;

    EGLContext getContext() const { return mContext; }
    EGLDisplay getDisplay() const { return mDisplay; }
    sk_sp<SkSurface> createSkiaSurface(int32_t width, int32_t height) REQUIRES(eglCtxMutex);
    GrDirectContext* getSkiaContext();
    EGLSurface getWindowSurface(ANativeWindow* window);

    EGLBoolean makeCurrent(EGLSurface surface = EGL_NO_SURFACE) REQUIRES(eglCtxMutex);

private:
    static std::weak_ptr<SkiaContextManager> mInstance;
    static std::mutex mMutex;

    sk_sp<GrDirectContext> mSkContext = nullptr;
    EGLDisplay mDisplay = EGL_NO_DISPLAY;
    EGLConfig mConfig = static_cast<EGLConfig>(0);
    EGLContext mContext = EGL_NO_CONTEXT;

    SkiaContextManager();
    ~SkiaContextManager();

    static void* getProcAddress(const char*);
    void makeSkiaContext();

    bool configHasAttribute(EGLConfig config, EGLint attribute, EGLint value) const;
};

} // namespace rive_android
