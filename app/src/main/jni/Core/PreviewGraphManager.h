//
//  ScreenshotGraphManager.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_PreviewGraphManager_h
#define QvodPlayer_PreviewGraphManager_h

#include "GraphManager.h"
#include <vector>
using std::vector;

class CPreviewGraphManager : public CGraphManager
{
public:
    CPreviewGraphManager(IDependency* pDepend, int* pResult);
    virtual ~CPreviewGraphManager();
    
    CMediaObject* GetComponent(const GUID& guid);
    int EnableComponent(const GUID& guid, BOOL bEnable);
    
protected:
    int BuildGraph();
    int DestroyGraph();
    
    virtual void OnInit(Argument& arg);
    virtual void OnLoaded(Argument& arg);
    virtual void OnOpened(Argument& arg);
    virtual void OnClose(Argument& arg);
    virtual void OnClosed(Argument& arg);
    virtual void OnPlay(Argument& arg);
    virtual void OnPlayed(Argument& arg);
    virtual void OnPause(Argument& arg);
    virtual void OnPaused(Argument& arg);
    virtual void OnFlush(Argument& arg);
    virtual void OnInvalid(Argument& arg);
    
    vector<CMediaObject*> m_vecObjs;
};

#endif
