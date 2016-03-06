//
//  PlayerInterface.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef PlayerInterface_h
#define PlayerInterface_h

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

// 外部请求的事件
#define REQUEST_OUTPUT_AUDIO               0
#define REQUEST_OUTPUT_VIDEO               1
#define REQUEST_INTERRUPT_AUDIO            2
    
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

typedef struct _PREVIEWINFO
{
    int       nBitRate;
    double    lfDuration;
    char      szArtist[64];
    char      szTitle[256];
    char      szAlbum[256];
    FRAMEINFO fi;
} PREVIEWINFO;
    
int CreatePlayer(const char* szPath);
int DestroyPlayer();
int Open(const char* pszURL, double lfOffset, int nRemote);
int Close();
int Play();
int Pause();
int Seek(double lfTime);
int CaptureFrame();
int StartPreview(const char* pszURL, double lfOffset, int nFrameCount);
int StopPreview();
int SetParameter(int nParam, void* pValue);
int GetParameter(int nParam, void* pValue);
int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved);
int SendRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved);

#ifdef __cplusplus
}
#endif

#endif
