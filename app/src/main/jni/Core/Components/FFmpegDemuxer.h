//
//  FFmpegDemuxer.h
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Demux_h
#define QVOD_Demux_h

#include <string.h>
using std::string;

#include "../MediaObject.h"
#include "CompInterfaces.h"
#include "FFmpegData.h"
#include "Pools.h"


class CFFmpegDemuxer : public CSource,
                       public CThread,
                       public IFFmpegDemuxer,
                       public IBufferingProbe
{
public:
    CFFmpegDemuxer(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CFFmpegDemuxer();
    
    // IFFmpegDemuxer
    virtual int InitialConfig(const char* szURL, double lfOffset, BOOL bRemote);
    virtual int ConnectedPeerNeedData(int nPeerType, BOOL bNeedData);
    virtual int SetSeekPosition(double lfOffset);
    virtual int GetMediaDuration(double* pDuration);
    virtual int GetMediaStartTime(double* pTime);
    virtual int GetMediaBitrate(int* pBitrate);
    virtual int GetMediaFormatName(char* pName);
    virtual int GetAudioChannelCount(int* pCount);
    virtual int GetAudioSampleFormat(int* pFormat);
    virtual int GetAudioTrackCount(int* pCount);
    virtual int GetAudioTimebase(double* lfTimebase);
    virtual int GetVideoTimebase(double* lfTimebase);
    virtual int GetCurAudioTrack(int* pTrack);
    virtual int SetCurAudioTrack(int nTrack);
    virtual int GetAudioSampleRate(double* pSampleRate);
    virtual int GetAudioFormatID(int* pFormatID);
    virtual int GetVideoFormatID(int* pFormatID);
    virtual int GetVideoFPS(int* pFPS);
    
    // IBufferingProbe
    virtual BOOL IsProbing();
    virtual BOOL IsNeedBuffering();
    
    BOOL IsRemoteFile();
    int GetSamplePool(const GUID& guid, ISamplePool** ppPool);
    
protected:
    // CMediaObject
    int Load();
    int WaitForResources(BOOL bWait);
    int Idle();
    int Execute();
    int Pause();
    int BeginFlush();
    int EndFlush();
    int Invalid();
    int Unload();
    int SetEOS();
    
    int Seek(double lfOffset);
    virtual THREAD_RETURN ThreadProc();
    
    virtual BOOL FillPacketPool(AVPacket* pPacket);
    virtual int RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx);
    
    BOOL PrepareCodecs(AVFormatContext* pFmtCtx);
    BOOL PrepareAudioData(AVFormatContext* pFmtCtx);
    BOOL PrepareVideoData(AVFormatContext* pFmtCtx);
    void UpdateSyncPoint(LONGLONG llTime);
    void UpdateSyncPoint2(LONGLONG llTime);
    BOOL ReadPacket(AVFormatContext* pFmtCtx, AVPacket* pPacket);
    void DuplicatePacket(AVPacket* pTo, const AVPacket* pFrom);
    void FillAudioPacketPool(AVPacket* pPktFrom);
    void FillVideoPacketPool(AVPacket* pPktFrom);
    void ReleaseResources();

    CPacketPool m_VideoPool;
    CPacketPool m_AudioPool;
    
    string      m_strURL;
    BOOL        m_bRemote;
    double      m_lfOffset;
    
    int         m_nJumpLimit;
    double      m_lfJumpBack;
    double      m_lfConvert;
    LONGLONG    m_llLastAudioTS;
    LONGLONG    m_llLastVideoTS;
    LONGLONG    m_llDisconThreshold;
    
    CEvent      m_sync;
    
    LONGLONG    m_llSyncPoint;
    LONGLONG    m_llStartTime;
    
    VideoInfo   m_video;
    AudioInfo   m_audio;
    FormatInfo  m_format;

private:
    void DiscardPackets(int nCount);

    BOOL        m_bDiscard;
};

#endif
