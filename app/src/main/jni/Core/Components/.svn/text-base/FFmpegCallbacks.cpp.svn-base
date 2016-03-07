//
//  FFmpegCallbacks.cpp
//  QVOD
//
//  Created by bigbug on 11-11-16.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "../Utinities.h"
#include "../Components.h"
#include "../QvodPlayer.h"
#include "../BufferingManager.h"
#include "../CallbackManager.h"
#include "CompInterfaces.h"
#include "FFmpegCallbacks.h"
using namespace::ios_qvod_player;

extern CCallbackManager* g_CallbackManager;

static int g_nInterrupt;
static CLock g_csInterrupt;

static int g_nRemote;
static int g_nLoopFilter;

int avio_interrupt_cb() // 通过url_set_interrupt_cb传给ffmpeg的回调函数，用于终止网络数据读写
{
    CAutoLock cObjectLock(&g_csInterrupt);
    return g_nInterrupt;
}

void interrupt_avio()
{
    CAutoLock cObjectLock(&g_csInterrupt);
    g_nInterrupt = 1;
}

void maintain_avio()
{
    CAutoLock cObjectLock(&g_csInterrupt);
    g_nInterrupt = 0;
}

int avio_is_remote()
{
    return g_nRemote;
}

void avio_set_remote(int nRemote)
{
    g_nRemote = nRemote;
}

int avcodec_is_loop_filter()
{
    return g_nLoopFilter;
}

void avcodec_enable_loop_filter(int nLoopFilter)
{
    g_nLoopFilter = nLoopFilter;
}

int notify_reconnect_cb(int64_t, void* pData)
{
    FormatInfo* pFormat = (FormatInfo*)pData;
    AVIOContext* pb = NULL;
    if (pFormat->pFormatContext) {
        pb = pFormat->pFormatContext->pb;
        if (pb == NULL) {
            return 0xFFFFFFFF;
        }
    } else {
        return 0xFFFFFFFF;
    }
    URLContext* h = (URLContext*)pb->opaque;

    double lfCurTime, lfDuration;
    CQvodPlayer::GetInstance()->GetParameter(PLAYER_GET_MEDIA_CURRENT_TIME, &lfCurTime);
    CQvodPlayer::GetInstance()->GetParameter(PLAYER_GET_MEDIA_DURATION, &lfDuration);
    
    if (avio_size(pb) - avio_tell(pb) <= 1024 * 2048 || lfDuration - lfCurTime <= 4) {
        return 0xFFFFFFFF;
    }
    
    int64_t llOffset = avio_tell(pb);
    int ret = ffurl_seek(h, llOffset, SEEK_SET);
    printf("reconnect url\n");
    
    return ret;
}

int notify_seek_pos_cb(int64_t llPos, void* pData)
{
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_SEEK_POSITION);
    
    CFFmpegDemuxer* pDemux = static_cast<CFFmpegDemuxer*>(pData);             
    if (llPos != 0) {
        (*cbd.pfnCallback)(cbd.pUserData, &llPos);
        //printf("set 5s accelerator\n");
    }
    
    return 0;
}

int notify_recv_size_cb(int64_t llSize, void* pData)
{
    CBufferingManager* pBufMgr = CBufferingManager::GetInstance();
    
    pBufMgr->UpdateBuffering(llSize);
    
    return 0;
}

int notify_buf_size_cb(int64_t llSize, void* pData)
{
    
    return 0;
}

int notify_read_index_cb(int64_t llPos, void* pData)
{    
    CallbackData cbd = g_CallbackManager->GetCallbackData(CALLBACK_READ_INDEX);
    
    (*cbd.pfnCallback)(cbd.pUserData, &llPos);
    
    return 0;
}

