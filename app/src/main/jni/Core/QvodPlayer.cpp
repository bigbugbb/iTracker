//
//  QvodPlayer.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>

#include "QvodPlayer.h"
#include "Message.h"
#include "Components.h"
#include "Components/FFmpegCallbacks.h"
#include "CallbackManager.h"
#include "PlayerGraphManager.h"
#include "PreviewGraphManager.h"

#ifdef LOG_VIDEO_RGB
    extern std::string strPathRGB;
    void LogRGB(void* pRGB, int nStride, int nHeight)
    {
        FILE* fp = fopen(strPathRGB.c_str(), "w+");
        for (int i = 0; i < nHeight; ++i)
            fwrite((BYTE*)pRGB + nStride * i, 1, nStride, fp);
        fclose(fp);
    }
    #define LOG_RGB(buf, stride, height) LogRGB(buf, stride, height)
#else 
    #define LOG_RGB(buf, stride, height)
#endif

struct FRAMEINFO
{
    FRAMEINFO()
    {
        memset(this, 0, sizeof(FRAMEINFO));
    }
    
    int    nWidth;
    int    nHeight;
    int    nStride;
    int    nFormat;
    void*  pContent;
};

struct PREVIEWINFO
{
    PREVIEWINFO()
    {
        memset(this, 0, sizeof(PREVIEWINFO));
    }
    
    int       nBitRate;
    double    lfDuration;
    char      szArtist[64];
    char      szTitle[256];
    char      szAlbum[256];
    FRAMEINFO fi;
};

CCallbackManager* g_CallbackManager = CCallbackManager::GetInstance();

CQvodPlayer::EventParam::EventParam()
{
    memset(this, 0, sizeof(EventParam));
}

CQvodPlayer::EventParam::EventParam(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData, void* pReserved)
{
    this->nEvent    = nEvent;
    this->dwParam1  = dwParam1;
    this->dwParam2  = dwParam2;
    this->pUserData = pUserData;
    this->pReserved = pReserved;
}

CQvodPlayer::CQvodPlayer(int* pResult)
{
    static CPlayerGraphManager s_GraphMgr(this, pResult);
    m_pPlayerManager = &s_GraphMgr;
    m_pDemuxer       = dynamic_cast<IFFmpegDemuxer*>(m_pPlayerManager->GetComponent(GUID_DEMUXER));
    m_pVideoDecoder  = dynamic_cast<IFFmpegVideoDecoder*>(m_pPlayerManager->GetComponent(GUID_VIDEO_DECODER));
    m_pAudioDecoder  = dynamic_cast<IFFmpegAudioDecoder*>(m_pPlayerManager->GetComponent(GUID_AUDIO_DECODER));
    m_pVideoRenderer = dynamic_cast<IVideoRenderer*>(m_pPlayerManager->GetComponent(GUID_VIDEO_RENDERER));
    m_pAudioRenderer = dynamic_cast<IAudioRenderer*>(m_pPlayerManager->GetComponent(GUID_AUDIO_RENDERER));
    
    static CPreviewGraphManager s_PreviewGraphMgr(this, pResult);
    m_pPreviewManager = &s_PreviewGraphMgr;
    m_pPreviewDemuxer = dynamic_cast<IFFmpegDemuxer*>(m_pPreviewManager->GetComponent(GUID_PREVIEW_DEMUXER));

    *pResult = S_OK;
}

CQvodPlayer::~CQvodPlayer()
{

}

CQvodPlayer* CQvodPlayer::GetInstance()
{
    int nResult = S_OK;
    static CQvodPlayer s_Player(&nResult);
    
    if (nResult != S_OK) {
        return NULL;
    }
    
    return &s_Player;
}

// IQvodPlayer
int CQvodPlayer::Open(const char* pszURL, double lfOffset, BOOL bRemote)
{
    Message msg(MSG_OPEN);
    
    m_pDemuxer->InitialConfig(pszURL, lfOffset, bRemote);
    m_pPlayerManager->SendMessage(msg);
    
    return S_OK;
}

int CQvodPlayer::Close()
{
    Message msg(MSG_CLOSE);
    
    m_pPlayerManager->ShrinkMessage(TRUE);
    m_pPlayerManager->SendMessage(msg);
    
//    interrupt_avio();
    
    return S_OK;
}

int CQvodPlayer::Play()
{
    Message msg(MSG_PLAY);
    
    m_pPlayerManager->SendMessage(msg);
    
    return S_OK;
}

int CQvodPlayer::Seek(double lfOffset)
{
    Log("Seek ####\n");
    Message msg(MSG_SEEK, TRUE);
    
    m_pDemuxer->SetSeekPosition(lfOffset);
    m_pVideoRenderer->SetMediaSeekTime(lfOffset);
    m_pAudioRenderer->SetMediaSeekTime(lfOffset);
    m_pPlayerManager->ShrinkMessage(); // discard seek message in the queue
//    interrupt_avio();
    
    m_pPlayerManager->SendMessage(msg);
    
    return S_OK;
}

int CQvodPlayer::Pause()
{    
    Message msg(MSG_PAUSE);
    
    m_pPlayerManager->SendMessage(msg);
    
    m_Respond[RESPOND_PAUSE].Wait();
    
    return S_OK;
}

int CQvodPlayer::WaitForResources(BOOL bWait, BOOL bCancel)
{
    Message msg(MSG_WAITFORRES, FALSE, bWait, bCancel);
    
    m_pPlayerManager->SendMessage(msg);
    
    return S_OK;
}

int CQvodPlayer::CaptureFrame()
{
    if (m_pVideoRenderer) {
        m_pVideoRenderer->EnableCaptureFrame(TRUE);
    }
    
    return S_OK;
}

int CQvodPlayer::StartPreview(const char* pszURL, double lfOffset, int nFrameCount)
{
    Message msg(MSG_OPEN);
    
    m_pPreviewDemuxer->InitialConfig(pszURL, lfOffset, 0);
    m_pPreviewManager->SendMessage(msg);
    
    if (nFrameCount > 1) {
        Message msg(MSG_PLAY);
        m_pPreviewManager->SendMessage(msg);
    }
    
    return S_OK;
}

int CQvodPlayer::StopPreview()
{
    Message msg(MSG_CLOSE);
    
    m_pPreviewManager->ShrinkMessage(TRUE);
    m_pPreviewManager->SendMessage(msg);
    
//    interrupt_avio();

    return S_OK;
}

int CQvodPlayer::SetParameter(int nParam, void* pValue)
{
    AssertValid(pValue);
    
    switch (nParam) {
    case PLAYER_SET_VIDEO_LOOP_FILTER:
        m_pVideoDecoder->EnableLoopFilter(*(BOOL*)pValue);
        break;
    default:
        break;
    }
    
    return S_OK;
}

int CQvodPlayer::GetParameter(int nParam, void* pValue)
{
    AssertValid(pValue);
    
    switch (nParam) {
    case PLAYER_GET_STATE:
        *(int*)pValue = m_pPlayerManager->GetState();
        break;
    case PLAYER_GET_MEDIA_DURATION:
        m_pDemuxer->GetMediaDuration((double*)pValue);
        break;
    case PLAYER_GET_MEDIA_CURRENT_TIME:
        m_pVideoRenderer->GetMediaCurrentTime((double*)pValue);
        break;
    case PLAYER_GET_MEDIA_BITRATE:
        m_pDemuxer->GetMediaBitrate((int*)pValue);
        break;
    case PLAYER_GET_MEDIA_FORMAT_NAME:
        m_pDemuxer->GetMediaFormatName((char*)pValue);
        break;
    case PLAYER_GET_AUDIO_FORMAT_ID:
        m_pDemuxer->GetAudioFormatID((int*)pValue);
        break;
    case PLAYER_GET_AUDIO_CHANNEL_COUNT:
        m_pDemuxer->GetAudioChannelCount((int*)pValue);
        break;
    case PLAYER_GET_AUDIO_TRACK_COUNT:
        m_pDemuxer->GetAudioTrackCount((int*)pValue);
        break;
    case PLAYER_GET_AUDIO_SAMPLE_FORMAT:
        m_pDemuxer->GetAudioSampleFormat((int*)pValue);
        break;
    case PLAYER_GET_AUDIO_SAMPLE_RATE:
        m_pDemuxer->GetAudioSampleRate((double*)pValue);
        break;
    case PLAYER_GET_AUDIO_CURRENT_TRACK:
        m_pDemuxer->GetCurAudioTrack((int*)pValue);
        break;
    case PLAYER_GET_VIDEO_FORMAT_ID:
        m_pDemuxer->GetVideoFormatID((int*)pValue);
        break;
    case PLAYER_GET_VIDEO_WIDTH:
        m_pVideoDecoder->GetVideoWidth((int*)pValue);
        break;
    case PLAYER_GET_VIDEO_HEIGHT:
        m_pVideoDecoder->GetVideoHeight((int*)pValue);
        break;
    case PLAYER_GET_VIDEO_FPS:
        m_pDemuxer->GetVideoFPS((int*)pValue);   
        break;
    default:
        break;
    }
    
    return S_OK;
}

int CQvodPlayer::SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved)
{
    AssertValid(g_CallbackManager);
    g_CallbackManager->SetCallback(nType, pfnCallback, pUserData, pReserved);
    
    return S_OK;
}

int CQvodPlayer::ReceiveRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved)
{
    switch (nType) {
    case REQUEST_OUTPUT_AUDIO:
        m_pAudioRenderer->OutputAudio((BYTE*)pUserData, *(UINT*)pReserved);
        break;
    case REQUEST_OUTPUT_VIDEO:
        m_pVideoRenderer->DeliverFrameReflection((BYTE*)pUserData, pReserved, nParam1);
        break;
    case REQUEST_INTERRUPT_AUDIO:
        m_pAudioRenderer->Interrupt((BOOL)nParam1);
        break;
    default:
        break;
    }
    
    return S_OK;
}

inline
int CQvodPlayer::FilterEvent(void* pSender, UINT nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData)
{
    return S_OK;
}

int CQvodPlayer::ReceiveEvent(void* pSender, int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData)
{
    if (FilterEvent(pSender, nEvent, dwParam1, dwParam2, pUserData) == E_HANDLED) {
        return S_OK;
    }
    
    EventParam param(nEvent, dwParam1, dwParam2, pUserData, NULL);
    
    switch (nEvent) {
    case EVENT_CREATE_AUDIO:
        OnCreateAudio(pSender, param);
        break;
    case EVENT_CREATE_VIDEO:
        OnCreateVideo(pSender, param);
        break;
    case EVENT_UPDATE_PICTURE_SIZE:
        OnUpdatePictureSize(pSender, param);
        break;
    case EVENT_DELIVER_FRAME:
        OnDeliverFrame(pSender, param);
        break;
    case EVENT_FRAME_CAPTURED:
        OnFrameCaptured(pSender, param);
        break;
    case EVENT_PREVIEW_CAPTURED:
        OnPreviewCaptured(pSender, param);
        break;     
    case EVENT_OPEN_FINISHED:
        OnOpenFinished(pSender, param);
        break;
    case EVENT_EXECUTE_FINISHED:
        OnExecuteFinished(pSender, param);
        break;
    case EVENT_PAUSE_FINISHED:
        OnPauseFinished(pSender, param);
        break;
    case EVENT_CLOSE_FINISHED:
        OnCloseFinished(pSender, param);
        break;
    case EVENT_PREVIEW_STARTED:
        OnPreviewStarted(pSender, param);
        break;
    case EVENT_PREVIEW_STOPPED:
        OnPreviewStopped(pSender, param);
        break;
    case EVENT_WAIT_FOR_RESOURCES:
        OnWaitForResources(pSender, param);
        break;
    case EVENT_ENCOUNTER_ERROR:
        OnEncounterError(pSender, param);
        break;
    case EVENT_AUDIO_ONLY:
        OnAudioOnly(pSender, param);
        break;
    case EVENT_VIDEO_ONLY:
        OnVideoOnly(pSender, param);
        break;
    case EVENT_DISCARD_VIDEO_PACKET:
        OnDiscardVideoPacket(pSender, param);
        break;
    case EVENT_AUDIO_NEED_DATA:
        OnAudioNeedData(pSender, param);
        break;
    case EVENT_AUDIO_EOS:
        OnAudioEOS(pSender, param);
        break;
    case EVENT_VIDEO_EOS:
        OnVideoEOS(pSender, param);
        break;
    case EVENT_CHECK_DEVICE:
        OnCheckDevice(pSender, param);
        break;
    default:
        return E_NOIMPL;
    }
    
    return E_HANDLED;
}

void CQvodPlayer::OnCreateAudio(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_CREATE_AUDIO_SERVICE);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}

void CQvodPlayer::OnCreateVideo(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_CREATE_VIDEO_SERVICE);
    DWORD dwDimension = param.dwParam1 | (param.dwParam2 << 16);
    
    (*cbd.pfnCallback)(cbd.pUserData, &dwDimension);
}

void CQvodPlayer::OnUpdatePictureSize(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_UPDATE_PICTURE_SIZE);
    DWORD dwDimension = param.dwParam1 | (param.dwParam2 << 16);
    
    (*cbd.pfnCallback)(cbd.pUserData, &dwDimension);
}

inline
void CQvodPlayer::OnDeliverFrame(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_DELIVER_FRAME);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);    
}

void CQvodPlayer::OnFrameCaptured(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_FRAME_CAPTURED);
    
    if (param.pUserData == NULL) {
        (*cbd.pfnCallback)(cbd.pUserData, NULL);
        return;
    }
    
    AVFrame* pFrame = (AVFrame*)param.pUserData;
    FRAMEINFO fi;
    
    fi.nWidth   = pFrame->width;
    fi.nHeight  = pFrame->height;
    fi.nStride  = pFrame->linesize[0];
    fi.nFormat  = pFrame->format;
    fi.pContent = pFrame->data[0]; // maybe not good, fixme
    
#ifdef LOG_RGB
    LOG_RGB(fi.pContent, fi.nStride, fi.nHeight);
#endif
    
    (*cbd.pfnCallback)(cbd.pUserData, &fi);
}

void CQvodPlayer::OnPreviewCaptured(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_PREVIEW_CAPTURED);
    
    if (param.pUserData == NULL) {
        (*cbd.pfnCallback)(cbd.pUserData, NULL);
        return;
    }
    
    AVFrame* pFrame = (AVFrame*)param.pUserData;
    IFFmpegDemuxer* pDemuxer = dynamic_cast<IFFmpegDemuxer*>(m_pPreviewManager->GetComponent(GUID_PREVIEW_DEMUXER));
    PREVIEWINFO pvi;
    
    pDemuxer->GetMediaBitrate(&pvi.nBitRate);
    pDemuxer->GetMediaDuration(&pvi.lfDuration);
    pvi.fi.nWidth   = pFrame->width;
    pvi.fi.nHeight  = pFrame->height;
    pvi.fi.nStride  = pFrame->linesize[0];
    pvi.fi.nFormat  = pFrame->format;
    pvi.fi.pContent = pFrame->data[0]; // maybe not good, fixme
    
    (*cbd.pfnCallback)(cbd.pUserData, &pvi);
}

void CQvodPlayer::OnOpenFinished(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_OPEN_FINISHED);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}

void CQvodPlayer::OnExecuteFinished(void* pSender, EventParam& param)
{
}

void CQvodPlayer::OnPauseFinished(void* pSender, EventParam& param)
{
    m_Respond[RESPOND_PAUSE].Signal();
}

void CQvodPlayer::OnCloseFinished(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_CLOSE_FINISHED);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}

void CQvodPlayer::OnPreviewStarted(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_PREVIEW_STARTED);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}

void CQvodPlayer::OnPreviewStopped(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_PREVIEW_STOPPED);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}

void CQvodPlayer::OnWaitForResources(void* pSender, EventParam& param)
{
    WaitForResources((BOOL)param.dwParam1, (BOOL)param.dwParam2);
}

void CQvodPlayer::OnEncounterError(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_ERROR);
    
    (*cbd.pfnCallback)(cbd.pUserData, (void*)param.dwParam1);
}

void CQvodPlayer::OnAudioOnly(void* pSender, EventParam& param)
{
    m_pPlayerManager->EnableComponent(GUID_VIDEO_RENDERER, FALSE);
    m_pPlayerManager->SetVideoEOS(TRUE);
}

void CQvodPlayer::OnVideoOnly(void* pSender, EventParam& param)
{
    m_pPlayerManager->EnableComponent(GUID_AUDIO_RENDERER, FALSE);
    m_pPlayerManager->SetAudioEOS(TRUE);
}

void CQvodPlayer::OnDiscardVideoPacket(void* pSender, EventParam& param)
{
    m_pVideoDecoder->DiscardPackets(param.dwParam1);
}

void CQvodPlayer::OnAudioNeedData(void* pSender, EventParam& param)
{
    m_pDemuxer->ConnectedPeerNeedData(CONNECTION_PEER_AUDIO, param.dwParam1);
}

void CQvodPlayer::OnAudioEOS(void* pSender, EventParam& param)
{
    m_pPlayerManager->SetAudioEOS(TRUE);
    if (m_pPlayerManager->IsVideoEOS()) {
        CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_PLAYBACK_FINISHED);
        (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
    }
}

void CQvodPlayer::OnVideoEOS(void* pSender, EventParam& param)
{
    m_pPlayerManager->SetVideoEOS(TRUE);
    if (m_pPlayerManager->IsAudioEOS()) {
        CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_PLAYBACK_FINISHED);
        (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
    }
}

void CQvodPlayer::OnCheckDevice(void* pSender, EventParam& param)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_CHECK_DEVICE);
    
    (*cbd.pfnCallback)(cbd.pUserData, param.pUserData);
}


