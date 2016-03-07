//
//  QvodPlayerInterface.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_QvodPlayerInterface_h
#define QVOD_QvodPlayerInterface_h

#ifdef __cplusplus
extern "C" {
#endif

#include "Core/Config.h"

// Android中向Java层发送的消息号
#define ON_OPENED					1
#define ON_CLOSED					2
#define ON_COMPLETION				3
#define ON_BEGIN_BUFFERING			4
#define ON_END_BUFFERING			5
#define ON_BUFFERING				6
#define ON_VIDEO_SIZE_CHANGED		7
#define ON_PREVIEW_STARTED		    8
#define ON_PREVIEW_CAPTURED			9
#define ON_PREVIEW_STOPPED			10
#define ON_NOTIFY_SEEK_POSITION		11
#define ON_NOTIFY_READ_INDEX		12
#define ON_ERROR       				13

// 用于获取或设置播放器属性
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

// 外部请求的事件
#define REQUEST_OUTPUT_AUDIO               0
#define REQUEST_OUTPUT_VIDEO               1
#define REQUEST_AUDIO_SWITCH               2
#define REQUEST_SUBTITLE_SWITCH            3
    
// 外部用到的错误码
#define S_OK                 0
#define E_FAIL              -1
#define E_NOIMPL            -2
#define E_OUTOFMEMORY       -3
#define E_IO                -4
#define E_BADSTREAM         -5
#define E_NOCODECS          -6
#define E_UNSUPPORTED       -7
#define E_BADPREVIEW        -8
    
// 外部可能用到的播放器状态码
#define STATE_LOADED              (1)
#define STATE_EXECUTE             (1 << 3)
#define STATE_PAUSE               (1 << 4)
#define STATE_INVALID             (1 << 5)
#define STATE_UNLOADED            (1 << 6)
#define STATE_NONE                (1 << 7)
    
// 需要实现的播放器外部回调函数码
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
typedef int (*PCallback)(void* pUserData, void* pReserved);

typedef struct _EMSG
{
    int    eID;
    void*  pParam1;
    void*  pParam2;
} EMSG;

typedef struct _FRAMEINFO
{
    int    nWidth;
    int    nHeight;
    int    nStride;
    int    nFormat;
    void*  pContent;
} FRAMEINFO;
    
typedef struct _MUSICINFO
{
    int       nSampleRate;
    char      szArtist[64];
    char      szTitle[256];
    char      szAlbum[256];
} MUSICINFO;

typedef struct _PREVIEWINFO
{
    int       nBitRate;
    float     fDuration;
    MUSICINFO mi;
    FRAMEINFO fi;
} PREVIEWINFO;

int CreatePlayer(const char* pszPath);
int DestroyPlayer();
int Open(const char* pszURL, float fOffset, int nRemote);
int Close();
int Play();
int Pause();
int Seek(float fTime);
int CaptureFrame();
int StartPreview(const char* pszURL, float fOffset, int nFrameCount);
int StopPreview();
int SetParameter(int nParam, void* pValue);
int GetParameter(int nParam, void* pValue);
int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved);
int SendRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved);

#ifdef __cplusplus
}
#endif

#endif
