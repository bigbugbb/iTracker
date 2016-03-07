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
    virtual int InitialConfig(const char* szURL, float fOffset, BOOL bRemote);
    virtual int SwitchAudioTrack(int nTrackID);
    virtual int SwitchSubtitleTrack(int nTrackID);
    virtual int SetSeekPosition(float fOffset);
    virtual int GetMediaDuration(float* pDuration);
    virtual int GetMediaStartTime(float* pTime);
    virtual int GetMediaBitrate(int* pBitrate);
    virtual int GetMediaFormatName(char* pName);
    virtual int GetAudioChannelCount(int* pCount);
    virtual int GetAudioSampleFormat(int* pFormat);
    virtual int GetAudioTrackCount(int* pCount);
    virtual int GetAudioTimebase(float* pTimebase);
    virtual int GetVideoTimebase(float* pTimebase);
    virtual int GetCurAudioTrack(int* pTrack);
    virtual int GetAudioSampleRate(float* pSampleRate);
    virtual int GetAudioCodecID(int* pCodecID);
    virtual int GetVideoCodecID(int* pCodecID);
    virtual int GetVideoFPS(float* pFPS);
    
    // IBufferingProbe
    virtual BOOL IsProbing();
    virtual BOOL IsNeedBuffering();
    
    BOOL IsRemoteFile();
    int GetOutputPool(const GUID& requestor, ISamplePool** ppPool);
    
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
    int Release();
    int SetEOS();
    int RespondFeedback(const GUID& sender, int nType, void* pUserData);
    
    int Seek(float fOffset);
    virtual THREAD_RETURN ThreadProc();
    
    virtual BOOL FillPacketPool(AVPacket* pPacket);
    virtual int RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx);
    
    BOOL PrepareCodecs(AVFormatContext* pFmtCtx);
    BOOL PrepareStreamData(AVFormatContext* pFmtCtx);
    BOOL PrepareAudioData(AVFormatContext* pFmtCtx);
    BOOL PrepareVideoData(AVFormatContext* pFmtCtx);
    BOOL PrepareSubtitleData(AVFormatContext* pFmtCtx);
    void UpdateSyncPoint(LONGLONG llTime);
    void UpdateSyncPoint2(LONGLONG llTime);
    BOOL ReadPacket(AVFormatContext* pFmtCtx, AVPacket* pPacket);
    void DuplicatePacket(AVPacket* pTo, const AVPacket* pFrom);
    void FillAudioPacketPool(CPacketPool* pPool, AVPacket* pPktFrom);
    void FillVideoPacketPool(CPacketPool* pPool, AVPacket* pPktFrom);
    void FillSubtitlePacketPool(CPacketPool* pPool, AVPacket* pPktFrom);
    void ReleaseResources();
    
    string       m_strURL;
    BOOL         m_bRemote;
    float        m_fOffset;
    
    int          m_nJumpLimit;
    float        m_fJumpBack;
    float        m_fConvertA;
    float        m_fConvertS;
    LONGLONG     m_llLastAudioTS;
    LONGLONG     m_llLastVideoTS;
    LONGLONG     m_llLastSubtitleTS;
    LONGLONG     m_llDisconThreshold;
    
    CEvent       m_sync;
    
    LONGLONG     m_llSyncPoint;
    LONGLONG     m_llStartTime;
    
    FormatInfo   m_format;
    
    CPacketPoolList m_PoolsV;
    CPacketPoolList m_PoolsA;
    CPacketPoolList m_PoolsS;

private:
    BOOL m_bDiscard;
};

#endif
