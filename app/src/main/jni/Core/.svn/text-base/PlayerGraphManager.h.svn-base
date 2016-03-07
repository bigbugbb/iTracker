//
//  QvodGraphManager.h
//  QVOD
//
//  Created by bigbug on 11-11-15.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_PlayerGraphManager_h
#define QVOD_PlayerGraphManager_h

#include "GraphManager.h"
#include <vector>
using std::vector;

class CBufferingManager;

class CPlayerGraphManager : public CGraphManager
{
public:
    CPlayerGraphManager(IDependency* pDepend, int* pResult);
    virtual ~CPlayerGraphManager();
    
    CMediaObject* GetComponent(const GUID& guid);
    int EnableComponent(const GUID& guid, BOOL bEnable);
    
    void SetAudioEOS(BOOL bEOS) { m_bAudioEOS = bEOS; }
    void SetVideoEOS(BOOL bEOS) { m_bVideoEOS = bEOS; }
    
    BOOL IsAudioEOS() const { return m_bAudioEOS; }
    BOOL IsVideoEOS() const { return m_bVideoEOS; }
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
    
    BOOL    m_bAudioEOS;
    BOOL    m_bVideoEOS;
private:
    CBufferingManager* m_pBufMgr;
};

#endif
