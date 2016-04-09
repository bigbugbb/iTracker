//
//  FFmpegVideoDecoder.cpp
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "FFmpegData.h"
#include "FFmpegCallbacks.h"
#include "FFmpegVideoDecoder.h"
using namespace std;

#define ALTER_THRESHOLD  350

CFFmpegVideoDecoder::CFFmpegVideoDecoder(const GUID& guid, IDependency* pDepend, int* pResult)
    : CMediaObject(guid, pDepend)
{
    m_nWidth  = -1;
    m_nHeight = -1;

    m_bWaitI = TRUE;
    m_pVideo = NULL;
    m_bJumpBack  = FALSE;
    m_pCodecCtx  = NULL;
    m_pFramePool = NULL;

    m_pYUV = av_frame_alloc();
    
#ifdef iOS
    m_pSwsCtx = NULL;
#endif
    m_eDstFmt = AV_PIX_FMT_RGB565;

    m_llLastInputTS  = AV_NOPTS_VALUE;
    m_llLastOutputTS = AV_NOPTS_VALUE;
    
    m_bLoopFilter = TRUE;
//    avcodec_set_loop_filter_probe_cb(avcodec_is_loop_filter);
    avcodec_enable_loop_filter(TRUE);
}

CFFmpegVideoDecoder::~CFFmpegVideoDecoder()
{
    if (m_pYUV) {
        av_frame_free(&m_pYUV);
    }
 }

int CFFmpegVideoDecoder::GetVideoWidth(int* pWidth)
{
    AssertValid(pWidth);
    if (m_nWidth == -1) {
        return E_FAIL;
    }
    *pWidth = m_nWidth;
    
    return S_OK;
}

int CFFmpegVideoDecoder::GetVideoHeight(int *pHeight)
{
    AssertValid(pHeight);
    if (m_nHeight == -1) {
        return E_FAIL;
    }
    *pHeight = m_nHeight;
    
    return S_OK;
}

int CFFmpegVideoDecoder::WaitKeyFrame(BOOL bWait)
{
    CAutoLock cObjectLock(&m_csDecode);
    
    m_bWaitI = bWait;
    m_nNonKeyCount = 0;
    
    return S_OK;
}

inline
BOOL CFFmpegVideoDecoder::IsWaitingKeyFrame()
{
    CAutoLock cObjectLock(&m_csDecode);
    
    return m_bWaitI;
}

int CFFmpegVideoDecoder::EnableLoopFilter(BOOL bEnable)
{
    m_bLoopFilter = bEnable;
    
    return S_OK;
}

inline
int CFFmpegVideoDecoder::EnableLoopFilter2(AVCodecContext* pCodecCtx)
{
    if (m_bLoopFilter) {
        m_pCodecCtx->skip_loop_filter = AVDISCARD_DEFAULT;
        avcodec_enable_loop_filter(TRUE);
    } else {
        m_pCodecCtx->skip_loop_filter = AVDISCARD_ALL;
        avcodec_enable_loop_filter(FALSE);
    }
    
    return S_OK;
}

int CFFmpegVideoDecoder::SetDecodeMode(int nDecMode)
{
    CAutoLock cObjectLock(&m_csDecode);
    
//    if (nDecMode == DECODE_MODE_I) {
//        //Log("Decode I\n");
//        m_pCodecCtx->skip_frame = AVDISCARD_NONKEY;
//    } else 
    if (nDecMode == DECODE_MODE_IP) {
        //Log("Decode IP\n");
//        if (m_pCodecCtx->skip_frame == AVDISCARD_NONKEY || 
//            m_pCodecCtx->skip_frame == AVDISCARD_ALL) {
//            //avcodec_flush_buffers(m_pCodecCtx);
//            WaitIFrame(TRUE);
//            if (m_pCodecCtx->codec_id == CODEC_ID_RV30 || m_pCodecCtx->codec_id == CODEC_ID_RV40) {
//                return S_OK;
//            }
//        }
        m_pCodecCtx->skip_frame = AVDISCARD_NONREF;
    } else if (nDecMode == DECODE_MODE_IPB) {
        //Log("Decode IPB\n");
        m_pCodecCtx->skip_frame = AVDISCARD_DEFAULT;
    } else if (nDecMode == DECODE_MODE_NONE) {
        avcodec_flush_buffers(m_pCodecCtx);
        m_pCodecCtx->skip_frame = AVDISCARD_ALL;
    }
    
    return S_OK;
}

int CFFmpegVideoDecoder::AlterQuality(LONGLONG llLate) // ms
{
    int nState = GetState();
    if (nState & STATE_PAUSE || nState & STATE_WAITFORRESOURCES) {
        llLate = 0;
    }
    
    if (llLate <= ALTER_THRESHOLD) {
        SetDecodeMode(DECODE_MODE_IPB);
    } else if (llLate > ALTER_THRESHOLD) {
        SetDecodeMode(DECODE_MODE_IP);
    } 
    
    return S_OK;
}

THREAD_RETURN CFFmpegVideoDecoder::ThreadProc()
{
    int nWait = 0;
    int nResult;
  
    while (m_bRun) {
        m_sync.Wait();    
        
        m_csDecode.Lock();
        nResult = Receive(m_pVideoPool);
        m_csDecode.Unlock();
        
        if (nResult == E_RETRY) {
            nWait = 20;
        } else if (nResult == E_FAIL) {
            nWait = 50;
        } else {
            nWait = 0;
        }
        
        m_sync.Signal();
        
        if (m_vecInObjs[0]->IsEOS() && !m_pVideoPool->GetSize()) {
            SetEOS();
        }
        
        Sleep(nWait);
    }
    
    return 0;
}

int CFFmpegVideoDecoder::OnReceive(CMediaSample& sample)
{
    AssertValid(sample.m_nSize == sizeof(AVPacket));
    AVPacket* pPacket = (AVPacket*)sample.m_pBuf;
    m_pCodecCtx = (AVCodecContext*)sample.m_pExten; // always the same
    m_pVideo = (VideoTrack*)sample.m_pSpecs;
    
    if (m_bFlush) {
        return E_RETRY;
    }
    
    EnableLoopFilter2(m_pCodecCtx);
    
    int nResult = Decode(pPacket, m_pCodecCtx, sample);
    if (nResult != E_RETRY) {
        align_free(pPacket->data);
        pPacket->data = NULL;
    }
    
    return nResult;
}

BOOL CFFmpegVideoDecoder::IsWaitingKeyFrameCanceled()
{
    return FALSE;
}

int CFFmpegVideoDecoder::Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn)
{
    CMediaSample sample;
    
    if (m_pFramePool->GetEmpty(sample) != S_OK) {
        return E_RETRY; // wait the decoded frames to be delivered
    }
    
    int nGotPic = 0;
    int nResult = avcodec_decode_video2(pCodecCtx, m_pYUV, &nGotPic, pPacket);

    CFrame& frame = *(CFrame*)sample.m_pExten;
    // resize & re-allocate the memory used for buffering decoded frames
    if (nGotPic && nResult > 0) {
        if (frame.m_nWidth != m_pYUV->width || frame.m_nHeight != m_pYUV->height) {
            int nResult = S_OK;
#ifdef ANDROID
            m_eDstFmt = pCodecCtx->pix_fmt; // on iOS, m_eDstFmt's default value is PIX_FMT_RGB565
#endif
            if ((nResult = frame.Resize(m_pYUV->width, m_pYUV->height, m_eDstFmt)) != S_OK) {
            	Log("CFFmpegVideoDecoder::Decode failed");
                return nResult;
            }
            if ((nResult = Resize(m_pYUV->width, m_pYUV->height, pCodecCtx->pix_fmt)) != S_OK) {
            	Log("CFFmpegVideoDecoder::Decode failed");
                return nResult;
            }
        }
    }
    
    float fJumpBack = (sampleIn.m_llTimestamp - sample.m_llSyncPoint) * m_pVideo->fTimebase;
    if (fJumpBack < 0) {
        m_bJumpBack = TRUE;
        SetDecodeMode(DECODE_MODE_IP);
    } else {
        if (m_bJumpBack) {
            m_bJumpBack = FALSE;
            SetDecodeMode(DECODE_MODE_IPB);
        }
    }

    if (m_pYUV->pict_type == AV_PICTURE_TYPE_I) {
        WaitKeyFrame(FALSE);
    } else if (m_pYUV->pict_type == AV_PICTURE_TYPE_NONE) {
        if (IsIntraOnly(pCodecCtx->codec_id)) WaitKeyFrame(FALSE);
    }
    
    frame.m_nType     = m_pYUV->pict_type;
    //Log("nGotPic: %d, waitI: %d, ignore: %d\n", nGotPic, !IsWaitingKeyFrame(), sampleIn.m_bIgnore);
    frame.m_bShow     = nGotPic && !IsWaitingKeyFrame() && !sampleIn.m_bIgnore;
    if (IsWaitingKeyFrame()) {
		++m_nNonKeyCount;
		if (IsWaitingKeyFrameCanceled()) {
			WaitKeyFrame(FALSE); frame.m_bShow = TRUE;
		}
	}
    frame.m_nDuration = pPacket->duration;
    if (frame.m_bShow) {
#ifdef iOS
        AVFrame* pRGB = &frame.m_frame;
        sws_scale(m_pSwsCtx, m_YUV.data, m_YUV.linesize, 0, m_nHeight, pRGB->data, pRGB->linesize);
#else
        av_picture_copy((AVPicture*)&frame.m_frame, (AVPicture*)m_pYUV, pCodecCtx->pix_fmt, m_nWidth, m_nHeight);
#endif
    }
    sample.m_bIgnore     = sampleIn.m_bIgnore;
    sample.m_llTimestamp = nGotPic ? AdjustTimestamp(m_pYUV->best_effort_timestamp, frame.m_nDuration) : pPacket->pts;
    sample.m_llSyncPoint = sampleIn.m_llSyncPoint;
    //Log("best effort ts: %lld, last input ts: %lld, last output ts: %lld **********\n", m_YUV.best_effort_timestamp, m_llLastInputTS, m_llLastOutputTS);
    m_llLastInputTS  = nGotPic ? m_pYUV->best_effort_timestamp : pPacket->pts;
    m_llLastOutputTS = sample.m_llTimestamp;
    AssertValid(!sampleIn.m_bIgnore);
    //Log("pts: %lld, syncpt: %lld, frame type: %d, show: %d, GOT: %d\n", sample.m_llTimestamp, sample.m_llSyncPoint, frame.m_nType, frame.m_bShow, nGotPic);

    m_pFramePool->Commit(sample);
    
    return S_OK;
}

inline
LONGLONG CFFmpegVideoDecoder::AdjustTimestamp(LONGLONG llTimestamp, int nDuration)
{
    if (m_llLastInputTS == AV_NOPTS_VALUE || m_llLastOutputTS == AV_NOPTS_VALUE) {
        if (llTimestamp == AV_NOPTS_VALUE) {
            llTimestamp = m_llLastOutputTS;
        }
        return llTimestamp;
    }
    
    if (FFABS(llTimestamp - m_llLastInputTS) < nDuration * 0.2) {
        llTimestamp = m_llLastOutputTS + nDuration;
    }
    
    if (llTimestamp == AV_NOPTS_VALUE) {
        llTimestamp = m_llLastOutputTS;
    }
    
    return llTimestamp;
}

int CFFmpegVideoDecoder::Resize(int nWidth, int nHeight, AVPixelFormat eSrcFmt)
{
    int nResult = S_OK;
    
    if (m_nWidth != nWidth || m_nHeight != nHeight) {
        m_nWidth = nWidth; m_nHeight = nHeight;
#ifdef iOS
        m_pSwsCtx = sws_getCachedContext(m_pSwsCtx, nWidth, nHeight, eSrcFmt, 
                nWidth, nHeight, m_eDstFmt, SWS_FAST_BILINEAR, NULL, NULL, NULL);
        if (!m_pSwsCtx) {
            return E_FAIL;
        }
#endif
        NotifyEvent(EVENT_UPDATE_PICTURE_SIZE, nWidth, nHeight, NULL);
        OnVideoSizeChanged();
    }

    return nResult;
}

int CFFmpegVideoDecoder::OnVideoSizeChanged()
{
    return S_OK;
}

BOOL CFFmpegVideoDecoder::IsIntraOnly(AVCodecID id)
{
    if (id == AV_CODEC_ID_MSVIDEO1 || id == AV_CODEC_ID_TSCC || id == AV_CODEC_ID_BINKVIDEO ||
        id == AV_CODEC_ID_BMP || id == AV_CODEC_ID_JPEG2000) { // should be more
        return TRUE;
    }
    
    return FALSE;
}

int CFFmpegVideoDecoder::Load()
{
    Log("CFFmpegVideoDecoder::Load\n");
    CMediaObject* pDemuxer = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        const GUID& guid = m_vecInObjs[i]->GetGUID();
        if (guid == GUID_DEMUXER) {
            pDemuxer = m_vecInObjs[i];
        }
    }
    if (!pDemuxer) {
        return E_FAIL;
    }
    pDemuxer->GetOutputPool(GetGUID(), &m_pVideoPool);
    
    if (!m_pVideoPool) {
        Log("No VideoData Pool!\n");
    } else {
        for (int i = 0; i < m_vecOutObjs.size(); ++i) {
            const GUID& guid = m_vecOutObjs[i]->GetGUID();
            if (guid == GUID_VIDEO_RENDERER) {
                m_pRenderer = m_vecOutObjs[i];
            }
        }
        if (!m_pRenderer) {
            return E_FAIL;
        }
        m_pRenderer->GetInputPool(GetGUID(), &m_pFramePool);
        if (!m_pFramePool) {
            return E_FAIL;
        }

        WaitKeyFrame(TRUE);
        Create();
    }
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CFFmpegVideoDecoder::WaitForResources(BOOL bWait)
{
    Log("CFFmpegVideoDecoder::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);

    return S_OK;
}

int CFFmpegVideoDecoder::Idle()
{
    Log("CFFmpegVideoDecoder::Idle\n");
    Start();
    
    CMediaObject::Idle();
    return S_OK;
}

int CFFmpegVideoDecoder::Execute()
{
    Log("CFFmpegVideoDecoder::Execute\n");
    
    CMediaObject::Execute();
    return S_OK;
}

int CFFmpegVideoDecoder::Pause()
{
    Log("CFFmpegVideoDecoder::Pause\n");
    
    CMediaObject::Pause();
    return S_OK;
}

int CFFmpegVideoDecoder::BeginFlush()
{
    Log("CFFmpegVideoDecoder::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_sync.Wait();
    
    return S_OK;
}

int CFFmpegVideoDecoder::EndFlush()
{
    Log("CFFmpegVideoDecoder::EndFlush\n");
    if (m_pCodecCtx) {
        m_pCodecCtx->skip_frame = AVDISCARD_DEFAULT;
        m_pCodecCtx->skip_loop_filter = AVDISCARD_DEFAULT;
    }
    
    m_llLastInputTS  = AV_NOPTS_VALUE;
    m_llLastOutputTS = AV_NOPTS_VALUE;
    
    WaitKeyFrame(TRUE);
    
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CFFmpegVideoDecoder::Invalid()
{
    Log("CFFmpegVideoDecoder::Invalid\n");
    CMediaObject::Invalid();
    
    Close();
    
    return S_OK;
}

int CFFmpegVideoDecoder::Unload()
{
    Log("CFFmpegVideoDecoder::Unload\n");
    Close();

    m_pRenderer  = NULL;
    m_pVideoPool = NULL;
    m_pFramePool = NULL;
    
    m_nWidth  = -1;
    m_nHeight = -1;
    m_bJumpBack   = FALSE;
    m_bLoopFilter = TRUE;
    m_llLastInputTS  = AV_NOPTS_VALUE;
    m_llLastOutputTS = AV_NOPTS_VALUE;
    
#ifdef iOS
    if (m_pSwsCtx) {
        sws_freeContext(m_pSwsCtx);
        m_pSwsCtx = NULL;
    }
#endif

    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegVideoDecoder::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    
    return CMediaObject::SetEOS();
}

int CFFmpegVideoDecoder::RespondDispatch(const GUID& sender, int nType, void* pUserData)
{
    if (nType == DISPATCH_DISCARD_PACKETS) {
        int nCount = *static_cast<int*>(pUserData);
        CAutoLock cObjectLock(&m_csDecode);
        
        CMediaSample sample;
        Log("in DiscardPackets\n");
        for (int i = 0; i < nCount; ++i) {
            int nResult = m_pVideoPool->GetUnused(sample);
            AssertValid(nResult == S_OK); // must be S_OK, or DiscardPackets should not be invoked
            
            AVPacket* pPacket = (AVPacket*)sample.m_pBuf;
            if (pPacket->data) {
                align_free(pPacket->data);
                pPacket->data = NULL;
            }
            
            m_pVideoPool->Recycle(sample);
        }
        WaitKeyFrame(TRUE);
        Log("out DiscardPackets\n");
    }
    
    return S_OK;
}






