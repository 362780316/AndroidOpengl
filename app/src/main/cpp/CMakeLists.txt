cmake_minimum_required(VERSION 3.4.1) # 总CMakeList已经有了

# opencv 头文件
include_directories(include)

# opencv 库文件
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

# 中科院人脸关键点库导入
add_subdirectory(FaceAlignment) # 直接引入  xxx.a库

include_directories(FaceAlignment/include) # 导入头文件  include<xxx.h>   否则：include"xxx/xxx/xxx/xxx.h"
include_directories(Eigen)

add_library( 
        native-lib
        SHARED
        FaceTrack.cpp
        native-lib.cpp
)

target_link_libraries( 
        native-lib
        log
        opencv_java4 # 链接OpenCV OpenCV 4.xx的，所以是这个 libopencv_java4.so
        seeta_fa_lib # 链接seeta

)