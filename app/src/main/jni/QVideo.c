#include <stdio.h>
#include <string.h>
#include "QVideo.h"

static video_t* video = NULL;

extern video_t video_android;

static void video_register_one(video_t* o)
{
    if (!video) {
        video = o;
        video->next = NULL;
    } else {
        video_t* temp = video;

        while (temp->next) {
            if (!strcmp(o->name, temp->name))
                return;
            temp = temp->next;
        }
        temp->next = o;
        temp->next->next = NULL;
    }
}

void video_register_all()
{
    video_register_one(&video_android);
}

video_t* video_find_by_name(const char* name)
{
    video_t* result;

    result = video;
    while (result) {
        if (!strcmp(result->name, name))
            break;
        result = result->next;
    }

    return result;
}

