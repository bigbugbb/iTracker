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

#define CONNECTION_PEER_AUDIO   0
#define CONNECTION_PEER_VIDEO   1
struct IFFmpegDemuxer
{
    virtual int InitialConfig(const char* szURL, double lfOffset, BOOL bRemote) = 0;
    virtual int ConnectedPeerNeedData(int nPeerType, BOOL bNeedData) = 0;
    virtual int SetSeekPosition(double lfOffset) = 0;
    virtual int GetMediaDuration(double* pDuration) = 0;
    virtual int GetMediaStartTime(double* pTime) = 0;
    virtual int GetMediaBitrate(int* pBitrate) = 0;
    virtual int GetMediaFormatName(char* pName) = 0;
    virtual int GetAudioChannelCount(int* pCount) = 0;
    virtual int GetAudioTrackCount(int* pCount) = 0;
    virtual int GetAudioSampleFormat(int* pFormat) = 0;
    virtual int GetAudioTimebase(double* lfTimebase) = 0;
    virtual int GetVideoTimebase(double* lfTimebase) = 0;
    virtual int GetCurAudioTrack(int* pTrack) = 0;
    virtual int SetCurAudioTrack(int nTrack) = 0;
    virtual int GetAudioSampleRate(double* pSampleRate) = 0;
    virtual int GetAudioFormatID(int* pFormatID) = 0;
    virtual int GetVideoFormatID(int* pFormatID) = 0;
    virtual int GetVideoFPS(int* pFPS) = 0;
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
    virtual int DiscardPackets(int nCount) = 0;
    virtual int EnableLoopFilter(BOOL bEnable) = 0;
};

struct IFFmpegAudioDecoder
{
    virtual int SetParameter(int nParam, void* pValue) = 0;
    virtual int GetParameter(int nParam, void* pValue) = 0;
};

struct IVideoRenderer
{
    virtual int SetTimebase(double lfTimebase) = 0;
    virtual int SetMediaSeekTime(double lfTime) = 0;
    virtual int GetMediaCurrentTime(double* pTime) = 0;
    virtual int SetMediaStartTime(double lfTime) = 0;
    virtual int EnableCaptureFrame(BOOL bCapture) = 0;
    virtual int DeliverFrameReflection(BYTE* pDst, void* pSrc, int nStride) = 0; // not used on iOS
};

struct IAudioRenderer
{
    virtual int SetTimebase(double lfTimebase) = 0;
    virtual int SetSampleRate(int nSampleRate) = 0;
    virtual int SetChannelCount(int nChannelCount) = 0;
    virtual int SetSampleFormat(int nSampleFormat) = 0;
    virtual int SetMediaSeekTime(double lfTime) = 0;
    virtual int GetMediaCurrentTime(double* pTime) = 0;
    virtual int SetMediaStartTime(double lfTime) = 0;
    virtual int Interrupt(BOOL bInterrupt) = 0;
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
