//
//  PlayerConsts.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_SysConsts_h
#define QVOD_SysConsts_h

#include "Config.h"

#define S_OK                 0  
#define E_FAIL              -1  
#define E_NOIMPL            -2 
#define E_OUTOFMEMORY       -3   
#define E_IO                -4 
#define E_BADSTREAM         -5
#define E_NOCODECS          -6
#define E_UNSUPPORTED       -7
#define E_BADPREVIEW        -8
#define E_RETRY             -9
#define E_HANDLED           -10
#define E_SHOWNEXT          -11

#define STATE_LOADED              (1)
#define STATE_WAITFORRESOURCES    (1 << 1)
#define STATE_IDLE                (1 << 2)
#define STATE_EXECUTE             (1 << 3)
#define STATE_PAUSE               (1 << 4)
#define STATE_INVALID             (1 << 5)
#define STATE_UNLOADED            (1 << 6)
#define STATE_NONE                (1 << 7)

#define REQUEST_OUTPUT_AUDIO            0
#define REQUEST_OUTPUT_VIDEO            1
#define REQUEST_INTERRUPT_AUDIO         2

#define CALLBACK_CREATE_AUDIO_SERVICE        0
#define CALLBACK_CREATE_VIDEO_SERVICE        1
#define CALLBACK_UPDATE_PICTURE_SIZE         2
#define CALLBACK_DELIVER_FRAME               3
#define CALLBACK_PLAYBACK_FINISHED           4
#define CALLBACK_ERROR                       5
#define CALLBACK_BEGIN_BUFFERING             6
#define CALLBACK_ON_BUFFERING                7
#define CALLBACK_END_BUFFERING               8
#define CALLBACK_SEEK_POSITION               9
#define CALLBACK_READ_INDEX                  10
#define CALLBACK_GET_DOWNLOAD_SPEED          11
#define CALLBACK_OPEN_FINISHED               12
#define CALLBACK_CLOSE_FINISHED              13
#define CALLBACK_PREVIEW_STARTED             14
#define CALLBACK_PREVIEW_STOPPED             15
#define CALLBACK_FRAME_CAPTURED              16
#define CALLBACK_PREVIEW_CAPTURED            17
#define CALLBACK_CHECK_DEVICE                18

#define PLAYER_GET_STATE                   0  //获得播放器状态：
#define PLAYER_GET_MEDIA_DURATION          1  //获得当前播放的音/视频长度(秒）
#define PLAYER_GET_MEDIA_CURRENT_TIME      2  //获得当前播放的音/视频时间点（秒）
#define PLAYER_GET_MEDIA_BITRATE           3
#define PLAYER_GET_MEDIA_FORMAT_NAME       4
#define PLAYER_GET_AUDIO_FORMAT_ID         5  //获得音频流的format id
#define PLAYER_GET_AUDIO_CHANNEL_COUNT     6
#define PLAYER_GET_AUDIO_TRACK_COUNT       7  //获得当前播放的音频流中channel总数
#define PLAYER_GET_AUDIO_SAMPLE_FORMAT     8
#define PLAYER_GET_AUDIO_SAMPLE_RATE       9  //获得音频的sample rate
#define PLAYER_GET_AUDIO_CURRENT_TRACK    10  //获得当前的音频channel索引号
#define PLAYER_GET_VIDEO_WIDTH            11  //获得当前图像的宽度（像素）
#define PLAYER_GET_VIDEO_HEIGHT           12  //获得当前图像的高度（像素）
#define PLAYER_GET_VIDEO_FORMAT_ID        13  //获得视频流的format id
#define PLAYER_GET_VIDEO_FPS              14  //获得视频流的fps
#define PLAYER_SET_VIDEO_LOOP_FILTER      15  

const int EVENT_CREATE_AUDIO            = 0;
const int EVENT_CREATE_VIDEO            = 1;
const int EVENT_UPDATE_MEDIA_START_TIME = 2;
const int EVENT_UPDATE_VIDEO_TIMEBASE   = 3;
const int EVENT_UPDATE_AUDIO_TIMEBASE   = 4;
const int EVENT_UPDATE_PICTURE_SIZE     = 5;
const int EVENT_DELIVER_FRAME           = 6;
const int EVENT_FRAME_CAPTURED          = 7;
const int EVENT_PREVIEW_CAPTURED        = 8;
const int EVENT_OPEN_FINISHED           = 9;
const int EVENT_EXECUTE_FINISHED        = 10;
const int EVENT_PAUSE_FINISHED          = 11;
const int EVENT_CLOSE_FINISHED         = 12;
const int EVENT_PREVIEW_STARTED         = 13;
const int EVENT_PREVIEW_STOPPED         = 14;
const int EVENT_WAIT_FOR_RESOURCES      = 15;
const int EVENT_ENCOUNTER_ERROR         = 16;
const int EVENT_AUDIO_EOS               = 17;
const int EVENT_VIDEO_EOS               = 18;
const int EVENT_AUDIO_ONLY              = 19;
const int EVENT_VIDEO_ONLY              = 20;
const int EVENT_DISCARD_VIDEO_PACKET    = 21;
const int EVENT_AUDIO_NEED_DATA         = 22;
const int EVENT_CHECK_DEVICE            = 23;

#endif
