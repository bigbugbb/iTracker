//
//  MediaGraph.cpp
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include <queue>
using namespace std;

#include "MediaGraph.h"
#include "MediaObject.h"


CMediaGraph::CMediaGraph(int* pResult)
{
    m_pGraph = new CGraph<GUID>();
    AssertValid(m_pGraph);
    if (!m_pGraph) {
        if (pResult) {
            *pResult = E_OUTOFMEMORY;
        }
    }
    
    m_pSource = NULL;
    m_pSink   = NULL;
}

CMediaGraph::~CMediaGraph()
{
    if (m_pGraph) {
        delete m_pGraph;
        m_pGraph = NULL;
    }
}

BOOL CMediaGraph::Traversal(CMediaObject* pObj, int nCommand, void* pParam, int& nResult)
{
    int nStart = -1, nNext = -1, nSize = GetSize();
    queue<CMediaObject*> qObjs;
    vector<BOOL> vecDirt(nSize, FALSE);
    
    nResult = E_FAIL;
    
    do {
        if (!pObj) {
            return FALSE;
        }
        
        if (!m_pGraph->Find(pObj->GetGUID(), nStart)) {
            return FALSE;
        }
        
        if (!vecDirt[nStart]) {
            if ((nResult = pObj->Operate(nCommand, pParam)) != S_OK) {
                return FALSE;
            }
            vecDirt[nStart] = TRUE;
        }
        
        for (nNext = m_pGraph->FirstV(nStart); nNext != -1; nNext = m_pGraph->NextV(nStart, nNext)) {
            pObj = m_mapObjs[m_pGraph->GetV(nNext)];
            AssertValid(pObj);
            qObjs.push(pObj);
        }
        
        if (qObjs.empty()) break;
        
        pObj = qObjs.front();   
        qObjs.pop();
        
    } while (1);
    
    return TRUE;
}

int CMediaGraph::GetSize() const
{
    int nSize = m_pGraph->Size(); // explicit for debug
    return nSize;
}

CMediaObject* CMediaGraph::GetSink() const
{
    return m_pSink;
}

CMediaObject* CMediaGraph::GetSource() const
{
    return m_pSource;
}

BOOL CMediaGraph::Insert(CMediaObject* pObj)
{
    AssertValid(pObj);
    GUID guid = pObj->GetGUID();
    
    if (!m_pGraph->InsertV(guid)) {
        return FALSE;
    }
    m_mapObjs[guid] = pObj;
    
    if (pObj->m_eType == SOURCE) {
        m_pSource = pObj;
    }
    
    return TRUE;
}

BOOL CMediaGraph::Connect(CMediaObject* pLeftObj, CMediaObject* pRightObj)
{
    AssertValid(pLeftObj && pRightObj);
    pLeftObj->Connect(DIR_OUT, pRightObj);
    pRightObj->Connect(DIR_IN, pLeftObj);
    
    return m_pGraph->InsertE(pLeftObj->GetGUID(), pRightObj->GetGUID());
}

BOOL CMediaGraph::Prepare()
{
    int nStart = -1, nNext = -1, nSize = GetSize();
    CMediaObject* pObj = m_pSource;
    queue<CMediaObject*> qObjs;
    vector<BOOL> vecDirt(nSize, FALSE);
  
    do {
        if (!pObj) {
            return FALSE;
        }
        
        if (!m_pGraph->Find(pObj->GetGUID(), nStart)) {
            return FALSE;
        }
        
        if (!vecDirt[nStart]) {
            m_pSink = pObj;
            vecDirt[nStart] = TRUE;
        }
        
        for (nNext = m_pGraph->FirstV(nStart); nNext != -1; nNext = m_pGraph->NextV(nStart, nNext)) {
            pObj = m_mapObjs[m_pGraph->GetV(nNext)];
            AssertValid(pObj);
            qObjs.push(pObj);
        }
        
        if (qObjs.empty()) break;
        
        pObj = qObjs.front();   
        qObjs.pop();
        
    } while (1);
    
    AssertValid(m_pSource && m_pSink);
    return TRUE;
}

CMediaObject* CMediaGraph::Find(const GUID& guid)
{
    if (!m_pGraph->Find(guid)) {
        return NULL;
    }
    
    return m_mapObjs[guid];
}

