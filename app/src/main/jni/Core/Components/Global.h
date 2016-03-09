//
//  Global.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Global_h
#define QVOD_Global_h

#define DEFAULT_ALIGN_BYTE  16

void* align_malloc(size_t size, size_t align);
void  align_free(void* ptr);

#endif
