#include <string.h>
#include <jni.h>
#include <time.h>
#include <pthread.h>
#include <android/log.h>
#include <assert.h>
#include "bsec_integration.h"
#include "bsec_serialized_configurations_iaq.h"

const char *TAG = "native-bsec";
const int LOOPER_POLL_DELAY_MS = 0;

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define ASSERT(cond, ...) if (!(cond)) { __android_log_assert(#cond, TAG, __VA_ARGS__);}

// processing callback to handler class
typedef struct bsec_context {
    JavaVM *javaVM;
    jclass jniNativeBsecClz;
    jobject jniNativeBsecObj;
    pthread_mutex_t lock;
    int done;
    float delay;
} BsecContext;
BsecContext g_ctx;


/**********************************************************************************************************************/
/* functions */
/**********************************************************************************************************************/

/*!
 * @brief           Write operation in either I2C or SPI
 *
 * param[in]        dev_addr        I2C or SPI device address
 * param[in]        reg_addr        register address
 * param[in]        reg_data_ptr    pointer to the data to be written
 * param[in]        data_len        number of bytes to be written
 *
 * @return          result of the bus communication function
 */
int8_t bus_write(uint8_t dev_addr, uint8_t reg_addr, uint8_t *reg_data_ptr, uint16_t data_len) {
    LOGE("bus_write dev_addr:%d reg_addr:%d data_len:%d", dev_addr, reg_addr, data_len);
    // ...
    // Please insert system specific function to write to the bus where BME680 is connected
    // ...
    JavaVM *javaVM = g_ctx.javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }
    jmethodID writeRegister = (*env).GetMethodID(g_ctx.jniNativeBsecClz,
                                                 "writeRegister", "(I[BI)I");

    LOGE("bus_write reg_data_ptr:%d", reg_data_ptr[0]);
    jbyteArray byteArray = env->NewByteArray(data_len);

    jbyte *data = env->GetByteArrayElements(byteArray, NULL);

    if (data != NULL) {
        LOGE("bus_write data:%d", data[0]);
        memcpy(data, reg_data_ptr, data_len);
        LOGE("bus_write data:%d", data[0]);

        env->SetByteArrayRegion(byteArray, 0, data_len, data);

        LOGE("bus_write byteArray:%s", byteArray[0]);
        env->ReleaseByteArrayElements(byteArray, data, JNI_ABORT);
        LOGE("bus_write data:%d reg_data_ptr:%d", data[0], reg_data_ptr[0]);

    } else {
        LOGE("bus_write GetByteArrayElements Error");

        return -1;
    }
    //env->GetByteArrayRegion(byteArray, 0, data_len, reg_data_ptr)
    int result = env->CallIntMethod(g_ctx.jniNativeBsecObj, writeRegister, reg_addr, byteArray,
                                    data_len);
    return result;
}

/*!
 * @brief           Read operation in either I2C or SPI
 *
 * param[in]        dev_addr        I2C or SPI device address
 * param[in]        reg_addr        register address
 * param[out]       reg_data_ptr    pointer to the memory to be used to store the read data
 * param[in]        data_len        number of bytes to be read
 *
 * @return          result of the bus communication function
 */
int8_t bus_read(uint8_t dev_addr, uint8_t reg_addr, uint8_t *reg_data_ptr, uint16_t data_len) {
    LOGE("bus_read dev_addr:%d reg_addr:%d data_len:%d", dev_addr, reg_addr, data_len);
    // ...
    // Please insert system specific function to read from bus where BME680 is connected
    // ...
    JavaVM *javaVM = g_ctx.javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }
    jmethodID readRegister = (*env).GetMethodID(g_ctx.jniNativeBsecClz,
                                                "readRegister", "(I[BI)I");

    jbyteArray byteArray = env->NewByteArray(data_len);

    LOGE("bus_read byteArray:%d", byteArray[0]);
    int result = env->CallIntMethod(g_ctx.jniNativeBsecObj, readRegister, reg_addr, byteArray,
                                    data_len);

    LOGE("bus_read byteArray:%d", byteArray[0]);

    jbyte *data = env->GetByteArrayElements(byteArray, NULL);


    if (data != NULL) {
        LOGE("bus_read data:%d", data[0]);
        memcpy(reg_data_ptr, data, data_len);
        LOGE("bus_read reg_data_ptr:%d data:%d", reg_data_ptr[0], data[0]);
        env->GetByteArrayRegion(byteArray, 0, data_len, data);

        env->ReleaseByteArrayElements(byteArray, data, JNI_ABORT);

        LOGE("bus_read data:%d", data[0]);
    } else {
        LOGE("bus_read GetByteArrayElements Error");
        return -1;
    }

    LOGE("bus_read reg_data_ptr:%d", reg_data_ptr[0]);
    //env->GetByteArrayRegion(byteArray, 0, data_len, reg_data_ptr)
    return result;;
}

/*!
 * @brief           System specific implementation of sleep function
 *
 * @param[in]       t_ms    time in milliseconds
 *
 * @return          none
 */
void sleep(uint32_t t_ms) {
    LOGE("sleep t_ms:%d", t_ms);
    // ...
    // Please insert system specific function sleep or delay for t_ms milliseconds
    // ...

    JavaVM *javaVM = g_ctx.javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return;
        }
    }
    jmethodID sleep = (*env).GetMethodID(g_ctx.jniNativeBsecClz,
                                         "sleep", "(I)V");
    env->CallVoidMethod(g_ctx.jniNativeBsecObj, sleep, t_ms);

}

/*!
 * @brief           Capture the system time in microseconds
 *
 * @return          system_current_time    current system timestamp in microseconds
 */
int64_t get_timestamp_us() {
    int64_t system_current_time = 0;
    // ...
    // Please insert system specific function to retrieve a timestamp (in microseconds)
    // ...
    struct timespec res{};
    clock_gettime(CLOCK_REALTIME, &res);
    system_current_time = 1000000.0 * res.tv_sec + (double) res.tv_nsec / 1e3;

    LOGE("get_timestamp_us :%lld", system_current_time);
    return system_current_time;
}

/*!
 * @brief           Handling of the ready outputs
 *
 * @param[in]       timestamp       time in nanoseconds
 * @param[in]       iaq             IAQ signal
 * @param[in]       iaq_accuracy    accuracy of IAQ signal
 * @param[in]       temperature     temperature signal
 * @param[in]       humidity        humidity signal
 * @param[in]       pressure        pressure signal
 * @param[in]       raw_temperature raw temperature signal
 * @param[in]       raw_humidity    raw humidity signal
 * @param[in]       gas             raw gas sensor signal
 * @param[in]       bsec_status     value returned by the bsec_do_steps() call
 *
 * @return          none
 */
void
output_ready(int64_t timestamp, float iaq, uint8_t iaq_accuracy, float temperature, float humidity,
             float pressure, float raw_temperature, float raw_humidity, float gas,
             bsec_library_return_t bsec_status,
             float static_iaq, float co2_equivalent, float breath_voc_equivalent) {
    LOGE("output_ready timestamp :%lld bsec_library_return_t:%d", timestamp, bsec_status);
    // ...
    // Please insert system specific code to further process or display the BSEC outputs
    // ...

    JavaVM *javaVM = g_ctx.javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return;
        }
    }
    jmethodID output_ready = (*env).GetMethodID(g_ctx.jniNativeBsecClz,
                                                "outputReady", "(JFIFFFFFFIFFF)V");
    env->CallVoidMethod(g_ctx.jniNativeBsecObj, output_ready, timestamp, iaq, iaq_accuracy,
                        temperature, humidity, pressure, raw_temperature, raw_humidity, gas,
                        bsec_status, static_iaq, co2_equivalent, breath_voc_equivalent);
}

/*!
 * @brief           Load previous library state from non-volatile memory
 *
 * @param[in,out]   state_buffer    buffer to hold the loaded state string
 * @param[in]       n_buffer        size of the allocated state buffer
 *
 * @return          number of bytes copied to state_buffer
 */
uint32_t state_load(uint8_t *state_buffer, uint32_t n_buffer) {
    LOGE("state_load n_buffer :%d", n_buffer);
    // ...
    // Load a previous library state from non-volatile memory, if available.
    //
    // Return zero if loading was unsuccessful or no state was available,
    // otherwise return length of loaded state string.
    // ...
    return 0;
}

/*!
 * @brief           Save library state to non-volatile memory
 *
 * @param[in]       state_buffer    buffer holding the state to be stored
 * @param[in]       length          length of the state string to be stored
 *
 * @return          none
 */
void state_save(const uint8_t *state_buffer, uint32_t length) {
    LOGE("state_save length :%d", length);
    // ...
    // Save the string some form of non-volatile memory, if possible.
    // ...
}

/*!
 * @brief           Load library config from non-volatile memory
 *
 * @param[in,out]   config_buffer    buffer to hold the loaded state string
 * @param[in]       n_buffer        size of the allocated state buffer
 *
 * @return          number of bytes copied to config_buffer
 */
uint32_t config_load(uint8_t *config_buffer, uint32_t n_buffer) {
    LOGE("config_load n_buffer :%d", n_buffer);
    // ...
    // Load a library config from non-volatile memory, if available.
    //
    // Return zero if loading was unsuccessful or no config was available,
    // otherwise return length of loaded config string.
    // ...
    //memcpy(config_buffer, bsec_config_iaq, sizeof(bsec_config_iaq));
    //return sizeof(bsec_config_iaq);
    return 0;
}


/*
 * processing one time initialization:
 *     Cache the javaVM into our context
 *     Find class ID for JniHelper
 *     Create an instance of JniHelper
 *     Make global reference since we are using them from a native thread
 * Note:
 *     All resources allocated here are never released by application
 *     we rely on system to free all global refs when it goes away;
 *     the pairing function JNI_OnUnload() never gets called at all.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGE("JNI_OnLoad");

    JNIEnv *env;
    memset(&g_ctx, 0, sizeof(g_ctx));

    g_ctx.javaVM = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    g_ctx.done = 0;
    g_ctx.jniNativeBsecObj = NULL;
    return JNI_VERSION_1_6;
}

/*
 * Main working thread function. From a pthread,
 *     calling back to MainActivity::updateTimer() to display ticks on UI
 *     calling back to JniHelper::updateStatus(String msg) for msg
 */
void *LoadBsecJNI(void *context) {

    auto *pctx = (BsecContext *) context;
    JavaVM *javaVM = pctx->javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void **) &env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, nullptr);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return nullptr;
        }
    }

    jmethodID bsec_init_result = (*env).GetMethodID(pctx->jniNativeBsecClz, "bsecInitResult", "(I)V");
    jmethodID bsec_finished = (*env).GetMethodID(pctx->jniNativeBsecClz, "bsecFinished", "(V)V");

    return_values_init ret;

    /* Call to the function which initializes the BSEC library
     * Switch on low-power mode and provide no temperature offset */
    ret = bsec_iot_init(pctx->delay, 0.0f, bus_write, bus_read, sleep,
                        state_load,
                        config_load);

    if (ret.bme680_status) {
        LOGE("Could not initialize BME680 %d", (int) ret.bme680_status);
        /* Could not initialize BME680 */
        env->CallVoidMethod(pctx->jniNativeBsecObj, bsec_init_result, (int) ret.bme680_status);
    } else if (ret.bsec_status) {
        LOGE("Could not initialize BSEC library %d", (int) ret.bsec_status);
        /* Could not initialize BSEC library */
        env->CallVoidMethod(pctx->jniNativeBsecObj, bsec_init_result, (int) ret.bsec_status);
    } else {
        LOGI("BSEC library initialized");
        env->CallVoidMethod(pctx->jniNativeBsecObj, bsec_init_result, (int) 0);
        /* Call to endless loop function which reads and processes data based on sensor settings */
        /* State is saved every 10.000 samples, which means every 10.000 * 3 secs = 500 minutes  */
        bsec_iot_loop(sleep, get_timestamp_us, output_ready, state_save, 10000);
        LOGI("BSEC library Stopped");
        env->CallVoidMethod(pctx->jniNativeBsecObj, bsec_finished);
    }

    (*javaVM).DetachCurrentThread();
    return context;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_krisbiketeam_smarthomeraspbpi3_units_Bme680BsecJNI_initBme680JNI(JNIEnv *env,
                                                                          jobject instance,
                                                                          jboolean short_delay) {
    pthread_t threadInfo_;
    pthread_attr_t threadAttr_;

    pthread_attr_init(&threadAttr_);
    pthread_attr_setdetachstate(&threadAttr_, PTHREAD_CREATE_DETACHED);

    pthread_mutex_init(&g_ctx.lock, NULL);

    jclass clz = env->GetObjectClass(instance);

    g_ctx.jniNativeBsecClz = reinterpret_cast<jclass>(env->NewGlobalRef(clz));
    g_ctx.jniNativeBsecObj = env->NewGlobalRef(instance);

    g_ctx.delay = BSEC_SAMPLE_RATE_ULP;
    if (short_delay) {
        g_ctx.delay = BSEC_SAMPLE_RATE_LP;
    }

    int result = pthread_create(&threadInfo_, &threadAttr_, LoadBsecJNI, &g_ctx);
    assert(result == 0);

    pthread_attr_destroy(&threadAttr_);

    (void) result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_krisbiketeam_smarthomeraspbpi3_units_Bme680BsecJNI_closeBme680JNI(JNIEnv *env,
                                                                           jobject thiz) {
    pthread_mutex_lock(&g_ctx.lock);
    g_ctx.done = 1;
    pthread_mutex_unlock(&g_ctx.lock);

    // waiting for ticking thread to flip the done flag
    struct timespec sleepTime;
    memset(&sleepTime, 0, sizeof(sleepTime));
    sleepTime.tv_nsec = 100000000;
    while (g_ctx.done) {
        nanosleep(&sleepTime, NULL);
    }

    // release object we allocated from initBme680JNI() function
    env->DeleteGlobalRef(g_ctx.jniNativeBsecObj);
    env->DeleteGlobalRef(g_ctx.jniNativeBsecClz);
    g_ctx.jniNativeBsecObj = NULL;
    g_ctx.jniNativeBsecClz = NULL;

    pthread_mutex_destroy(&g_ctx.lock);
}