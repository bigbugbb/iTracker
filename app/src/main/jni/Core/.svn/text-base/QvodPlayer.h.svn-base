//
//  QvodPlayer.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_QvodPlayer_h
#define QVOD_QvodPlayer_h

#include "IQvodPlayer.h"
#include "DependencyObject.h"
#include "Components/CompInterfaces.h"
using ios_qvod_player::CLock;

enum {
    RESPOND_PLAY = 0,
    RESPOND_PAUSE,
    RESPOND_COUNT
};

struct Message;
struct IReferenceClock;
class CPlayerGraphManager;
class CPreviewGraphManager;

class CQvodPlayer : public IQvodPlayer, 
                    public IDependency, 
                    public CDependencyObject
{
    CQvodPlayer(int* pResult);
    virtual ~CQvodPlayer();
    
public:
    static CQvodPlayer* GetInstance();
    
    // IQvodPlayer
    virtual int Open(const char* pszURL, double lfOffset, BOOL bRemote);
    virtual int Close();
    virtual int Play();
    virtual int Seek(double lfOffset);
    virtual int Pause();
    virtual int CaptureFrame();
    virtual int StartPreview(const char* pszURL, double lfOffset, int nFrameCount);
    virtual int StopPreview();
    virtual int SetParameter(int nParam, void* pValue);
    virtual int GetParameter(int nParam, void* pValue);
    virtual int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved);
    virtual int ReceiveRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved);

    // IDependency
    virtual int ReceiveEvent(void* pSender, int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData);
    
protected:
    int SendMessage(const Message& msg);
    int WaitForResources(BOOL bWait, BOOL bCancel = FALSE);
    
    // structure to hold event parameters
    struct EventParam
    {
        EventParam();
        EventParam(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData, void* pReserved);
        
        int    nEvent;
        DWORD  dwParam1;
        DWORD  dwParam2;
        void*  pUserData;
        void*  pReserved;
    };
    
    virtual int FilterEvent(void* pSender, UINT nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData);
    
    // Some event handlers
    void OnCreateAudio(void* pSender, EventParam& param);
    void OnCreateVideo(void* pSender, EventParam& param);
    void OnUpdatePictureSize(void* pSender, EventParam& param);
    void OnDeliverFrame(void* pSender, EventParam& param);
    void OnFrameCaptured(void* pSender, EventParam& param);
    void OnPreviewCaptured(void* pSender, EventParam& param);
    void OnOpenFinished(void* pSender, EventParam& param);
    void OnExecuteFinished(void* pSender, EventParam& param);
    void OnPauseFinished(void* pSender, EventParam& param);
    void OnCloseFinished(void* pSender, EventParam& param);
    void OnPreviewStarted(void* pSender, EventParam& param);
    void OnPreviewStopped(void* pSender, EventParam& param);
    void OnWaitForResources(void* pSender, EventParam& param);
    void OnEncounterError(void* pSender, EventParam& param);
    void OnAudioOnly(void* pSender, EventParam& param);
    void OnVideoOnly(void* pSender, EventParam& param);
    void OnDiscardVideoPacket(void* pSender, EventParam& param);
    void OnAudioNeedData(void* pSender, EventParam& param);
    void OnAudioEOS(void* pSender, EventParam& param);
    void OnVideoEOS(void* pSender, EventParam& param);
    void OnCheckDevice(void* pSender, EventParam& param);
    
    CEvent  m_Respond[RESPOND_COUNT];
    
    IFFmpegDemuxer*       m_pDemuxer;
    IFFmpegVideoDecoder*  m_pVideoDecoder;
    IFFmpegAudioDecoder*  m_pAudioDecoder;
    IVideoRenderer*       m_pVideoRenderer;
    IAudioRenderer*       m_pAudioRenderer;
    IFFmpegDemuxer*       m_pPreviewDemuxer;
    
    CPlayerGraphManager*  m_pPlayerManager;
    CPreviewGraphManager* m_pPreviewManager;
};

#endif
