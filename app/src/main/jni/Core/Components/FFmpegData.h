//
//  FFmpegData.h
//  QVOD
//
//  Created by bigbug on 11-11-15.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_FFmpegData_h
#define QVOD_FFmpegData_h

#ifdef __cplusplus
extern "C" {
#endif
    
#include "libavformat/avformat.h"
//#include "libavformat/url.h"
#include "libswscale/swscale.h"
#include "libavutil/mathematics.h"
#include "libavcodec/avcodec.h"
//#include "libswresample/swresample_internal.h"
    
#ifdef __cplusplus
}
#endif

#include <vector>
using std::vector;

#define AVCODEC_MAX_AUDIO_FRAME_SIZE 192000

typedef enum AVSampleFormat SampleFormat;

struct VideoTrack
{
    VideoTrack()
    {
        memset(this, 0, sizeof(VideoTrack));
    }
    
    int          nStreamID;
    int          nCodecID;
    int          nWidth;
    int          nHeight;
    int          nBitrate;
    int64_t      llDuration;
    float        fFPS;
    float        fTimebase;
    void*        pUserData;
    
    AVCodec*         pCodec;
    AVCodecContext*  pCodecCtx;
};

struct AudioTrack
{
    AudioTrack()
    {
        memset(this, 0, sizeof(AudioTrack));
    }
    
    int          nStreamID;
    int          nCodecID;
    int          nSampleRate;
    int          nChannelsPerFrame;
    int          nFramesPerPacket;
    int          nBitrate;
    int64_t      llDuration;
    SampleFormat nSampleFormat;
    float        fTimebase;
    char         szTitle[256];
    char         szAlbum[256];
    char         szArtist[64];
    void*        pUserData;
    
    AVCodec*         pCodec;
    AVCodecContext*  pCodecCtx;
};

struct SubtitleTrack
{
    SubtitleTrack()
    {
        memset(this, 0, sizeof(SubtitleTrack));
    }
    
    int          nStreamID;
    int          nCodecID;
    float        fTimebase;
    void*        pUserData;
    
    AVCodec*         pCodec;
    AVCodecContext*  pCodecCtx;
};

typedef vector<VideoTrack>    VideoTracks;
typedef vector<AudioTrack>    AudioTracks;
typedef vector<SubtitleTrack> SubtitleTracks;

struct FormatInfo
{
    FormatInfo()
    {
        Clear();
    }
    
    void Clear() 
    {
        pFmtCtx  = NULL;
        bDecodeV = TRUE;
        bDecodeA = TRUE;
        bDecodeS = TRUE;
        
        nCurTrackV = 0;
        nCurTrackA = 0;
        nCurTrackS = 0;
        
        tracksV.clear();
        tracksA.clear();
        tracksS.clear();
    }
    
    VideoTrack* GetCurVideoTrack()
    {
        if (nCurTrackV < 0 || nCurTrackV >= tracksV.size()) {
            return NULL;
        }
        
        return &tracksV[nCurTrackV];
    }
    
    AudioTrack* GetCurAudioTrack()
    {
        if (nCurTrackA < 0 || nCurTrackA >= tracksA.size()) {
            return NULL;
        }
        
        return &tracksA[nCurTrackA];
    }
    
    SubtitleTrack* GetCurSubtitleTrack()
    {
        if (nCurTrackS < 0 || nCurTrackS >= tracksS.size()) {
            return NULL;
        }
        
        return &tracksS[nCurTrackS];
    }
    
    AVFormatContext* pFmtCtx;
    
    VideoTracks      tracksV;
    AudioTracks      tracksA;
    SubtitleTracks   tracksS;
    
    int   nCurTrackV;
    int   nCurTrackA;
    int   nCurTrackS;
    
    bool  bDecodeV;
    bool  bDecodeA;
    bool  bDecodeS;
};

#endif
