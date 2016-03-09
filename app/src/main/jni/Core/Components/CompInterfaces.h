//
//  CompInterfaces.h
//  QVOD
//
//  Created by bigbug on 11-11-16.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_CompInterfaces_h
#define QVOD_CompInterfaces_h


struct IBufferingProbe
{
    virtual BOOL IsProbing() = 0;
    virtual BOOL IsNeedBuffering() = 0;
};

struct IQualityControl
{
    virtual int AlterQuality(LONGLONG llLate) = 0;
};

struct IFrameCapturer
{
    virtual int CaptureFrame() = 0;
};

struct IFFmpegDemuxer
{
    virtual int InitialConfig(const char* szURL, float fOffset, BOOL bRemote) = 0;
    virtual int SwitchAudioTrack(int nTrackID) = 0;
    virtual int SwitchSubtitleTrack(int nTrackID) = 0;
    virtual int SetSeekPosition(float fOffset) = 0;
    virtual int GetMediaDuration(float* pDuration) = 0;
    virtual int GetMediaStartTime(float* pTime) = 0;
    virtual int GetMediaBitrate(int* pBitrate) = 0;
    virtual int GetMediaFormatName(char* pName) = 0;
    virtual int GetAudioChannelCount(int* pCount) = 0;
    virtual int GetAudioTrackCount(int* pCount) = 0;
    virtual int GetAudioSampleFormat(int* pFormat) = 0;
    virtual int GetAudioTimebase(float* pTimebase) = 0;
    virtual int GetVideoTimebase(float* pTimebase) = 0;
    virtual int GetCurAudioTrack(int* pTrack) = 0;
    virtual int GetAudioSampleRate(float* pSampleRate) = 0;
    virtual int GetAudioCodecID(int* pCodecID) = 0;
    virtual int GetVideoCodecID(int* pCodecID) = 0;
    virtual int GetVideoFPS(float* pFPS) = 0;
};

#define DECODE_MODE_NONE    0
#define DECODE_MODE_I       1
#define DECODE_MODE_IP      2
#define DECODE_MODE_IPB     3
struct IFFmpegVideoDecoder
{
    virtual int GetVideoWidth(int* pWidth) = 0;
    virtual int GetVideoHeight(int* pHeight) = 0;
    virtual int SetDecodeMode(int nDecMode) = 0;
    virtual int EnableLoopFilter(BOOL bEnable) = 0;
};

struct IFFmpegAudioDecoder
{
};

struct IFFmpegSubtitleDecoder
{
};

struct IVideoRenderer
{
    virtual int SetTimebase(float fTimebase) = 0;
    virtual int SetMediaSeekTime(float fTime) = 0;
    virtual int GetMediaCurrentTime(float* pTime) = 0;
    virtual int SetMediaStartTime(float fTime) = 0;
    virtual int EnableCaptureFrame(BOOL bCapture) = 0;
    virtual int DeliverFrameReflection(BYTE* pDst, void* pSrc, int nStride, int nHeight) = 0; // not used on iOS
};

struct IAudioRenderer
{
    virtual int SetTimebase(float fTimebase) = 0;
    virtual int SetSampleRate(int nSampleRate) = 0;
    virtual int SetChannelCount(int nChannelCount) = 0;
    virtual int SetSampleFormat(int nSampleFormat) = 0;
    virtual int SetMediaSeekTime(float fTime) = 0;
    virtual int GetMediaCurrentTime(float* pTime) = 0;
    virtual int SetMediaStartTime(float fTime) = 0;
    virtual int OutputAudio(BYTE* pData, unsigned int nDataByteSize) = 0;
};

struct IPreviewDemuxer : public IFFmpegDemuxer
{
    
};

struct IPreviewVideoDecoder : public IFFmpegVideoDecoder
{
    
};

struct IPreviewVideoRenderer : public IVideoRenderer
{

};

#endif
