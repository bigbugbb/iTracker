//
//  BaseTypes.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-24.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "BaseTypes.h"


BOOL operator<(const GUID& left, const GUID& right) 
{
    return left.Data1 < right.Data1;
}

BOOL operator>(const GUID& left, const GUID& right) 
{
    return left.Data1 > right.Data1;
}

BOOL operator==(const GUID& left, const GUID& right) 
{
    return !memcmp(&left, &right, sizeof(GUID));
}