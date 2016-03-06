//
//  ComponentsListener.cpp
//  QVOD
//
//  Created by bigbug on 11-12-31.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "../SysConsts.h"
#include "../MediaObject.h"
#include "FFmpegData.h"
#include "FFmpegVideoDecoder.h"
#include "FFmpegAudioDecoder.h"
#include "VideoRenderer.h"
#include "AudioRenderer.h"


//int VideoDecoder_WaitKeyFrame(CMediaObject* pObj, void* pParam)
//{
//    CFFmpegVideoDecoder* pDecoder = (CFFmpegVideoDecoder*)pObj;
    
//    pDecoder->WaitKeyFrame(TRUE);
    
//    return S_OK;
//}