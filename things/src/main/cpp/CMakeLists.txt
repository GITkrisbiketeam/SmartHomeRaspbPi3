cmake_minimum_required(VERSION 3.4.1)

add_library(native-bsec SHARED
            native-bsec.cpp
            include/bsec_integration.c
            include/bme680.c
            include/config/generic_33v_300s_4d/bsec_serialized_configurations_iaq.c)

target_include_directories(native-bsec PRIVATE
                            ${PROJECT_DIR}/things/src/main/cpp/include
                            ${PROJECT_DIR}/things/src/main/cpp/include/config/generic_33v_300s_4d
                            ${PROJECT_DIR}/things/src/main/cpp/libs/bsec/${CMAKE_ANDROID_ARCH_ABI})

add_library(algobse STATIC IMPORTED)
set_target_properties(algobse PROPERTIES IMPORTED_LOCATION ${PROJECT_DIR}/things/src/main/cpp/libs/bsec/${CMAKE_ANDROID_ARCH_ABI}/libalgobsec.a)


target_link_libraries(native-bsec
        android
        log
        algobse)

