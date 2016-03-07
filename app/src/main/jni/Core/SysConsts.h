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

#define INT8_MAX         127
#define INT16_MAX        32767
#define INT32_MAX        2147483647
#define INT64_MAX        9223372036854775807LL

#define INT8_MIN          -128
#define INT16_MIN         -32768
/*
 Note:  the literal "most negative int" cannot be written in C --
 the rules in the standard (section 6.4.4.1 in C99) will give it
 an unsigned type, so INT32_MIN (and the most negative member of
 any larger signed type) must be written via a constant expression.
 */
#define INT32_MIN        (-INT32_MAX-1)
#define INT64_MIN        (-INT64_MAX-1)

#define UINT8_MAX         255
#define UINT16_MAX        65535
#define UINT32_MAX        4294967295U
#define UINT64_MAX        18446744073709551615ULL

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

#define REQUEST_OUTPUT_AUDIO               0
#define REQUEST_OUTPUT_VIDEO               1
#define REQUEST_AUDIO_SWITCH               2
#define REQUEST_SUBTITLE_SWITCH            3

#define CALLBACK_CREATE_AUDIO_SERVICE        0
#define CALLBACK_CREATE_VIDEO_SERVICE        1
#define CALLBACK_RESET_AUDIO_SERVICE         2
#define CALLBACK_UPDATE_PICTURE_SIZE         3
#define CALLBACK_DELIVER_FRAME               4
#define CALLBACK_PLAYBACK_FINISHED           5
#define CALLBACK_ERROR                       6
#define CALLBACK_BEGIN_BUFFERING             7
#define CALLBACK_ON_BUFFERING                8
#define CALLBACK_END_BUFFERING               9
#define CALLBACK_SEEK_POSITION               10
#define CALLBACK_READ_INDEX                  11
#define CALLBACK_GET_DOWNLOAD_SPEED          12
#define CALLBACK_OPEN_FINISHED               13
#define CALLBACK_CLOSE_FINISHED              14
#define CALLBACK_PREVIEW_STARTED             15
#define CALLBACK_PREVIEW_STOPPED             16
#define CALLBACK_FRAME_CAPTURED              17
#define CALLBACK_PREVIEW_CAPTURED            18
#define CALLBACK_CHECK_DEVICE                19
#define CALLBACK_BEGIN_SUBTITLE              20
#define CALLBACK_END_SUBTITLE                21

#define PLAYER_GET_STATE                   0  //获得播放器状态：
#define PLAYER_GET_PREVIEW_STATE           1
#define PLAYER_GET_MEDIA_DURATION          2  //获得当前播放的音/视频长度(秒）
#define PLAYER_GET_MEDIA_CURRENT_TIME      3  //获得当前播放的音/视频时间点（秒）
#define PLAYER_GET_MEDIA_BITRATE           4
#define PLAYER_GET_MEDIA_FORMAT_NAME       5
#define PLAYER_GET_AUDIO_CODEC_ID          6  //获得音频流的codec id
#define PLAYER_GET_AUDIO_CHANNEL_COUNT     7
#define PLAYER_GET_AUDIO_TRACK_COUNT       8  //获得当前播放的音频流中channel总数
#define PLAYER_GET_AUDIO_SAMPLE_FORMAT     9
#define PLAYER_GET_AUDIO_SAMPLE_RATE      10  //获得音频的sample rate
#define PLAYER_GET_AUDIO_CURRENT_TRACK    11  //获得当前的音频channel索引号
#define PLAYER_GET_VIDEO_WIDTH            12  //获得当前图像的宽度（像素）
#define PLAYER_GET_VIDEO_HEIGHT           13  //获得当前图像的高度（像素）
#define PLAYER_GET_VIDEO_CODEC_ID         14  //获得视频流的codec id
#define PLAYER_GET_VIDEO_FPS              15  //获得视频流的fps
#define PLAYER_SET_VIDEO_LOOP_FILTER      16  

const int EVENT_CREATE_AUDIO            = 0;
const int EVENT_CREATE_VIDEO            = 1;
const int EVENT_RESET_AUDIO             = 2;
const int EVENT_UPDATE_MEDIA_START_TIME = 3;
const int EVENT_UPDATE_VIDEO_TIMEBASE   = 4;
const int EVENT_UPDATE_AUDIO_TIMEBASE   = 5;
const int EVENT_UPDATE_PICTURE_SIZE     = 6;
const int EVENT_DELIVER_FRAME           = 7;
const int EVENT_FRAME_CAPTURED          = 8;
const int EVENT_PREVIEW_CAPTURED        = 9;
const int EVENT_OPEN_FINISHED           = 10;
const int EVENT_EXECUTE_FINISHED        = 11;
const int EVENT_PAUSE_FINISHED          = 12;
const int EVENT_CLOSE_FINISHED          = 13;
const int EVENT_PREVIEW_STARTED         = 14;
const int EVENT_PREVIEW_STOPPED         = 15;
const int EVENT_WAIT_FOR_RESOURCES      = 16;
const int EVENT_ENCOUNTER_ERROR         = 17;
const int EVENT_AUDIO_EOS               = 18;
const int EVENT_VIDEO_EOS               = 19;
const int EVENT_AUDIO_ONLY              = 20;
const int EVENT_VIDEO_ONLY              = 21;
const int EVENT_CHECK_DEVICE            = 22;
const int EVENT_BEGIN_SUBTITLE          = 23;
const int EVENT_END_SUBTITLE            = 24;

const int FEEDBACK_SWITCH_AUDIO      = 0;
const int FEEDBACK_SUFFICIENT_DATA   = 1;
const int FEEDBACK_INSUFFICIENT_DATA = 2;

const int DISPATCH_DISCARD_PACKETS = 0;
const int DISPATCH_SWITCH_AUDIO    = 1;
const int DISPATCH_SWITCH_SUBTITLE = 2;

#endif
