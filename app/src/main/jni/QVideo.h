
#ifndef video_H
#define video_H

//#include "libavutil/pixfmt.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    int cvt;
    int width;
    int height;
    double pts;
} Picture;

typedef struct _video_ {
    char* name;
    int  (*init)(int w,int h);
    void (*free)();
    int  (*display)(void* pFrame, int mode);
    struct _video_* next;
} video_t;

void video_register_all();

video_t* video_find_by_id(int id);
video_t* video_find_by_name(const char* name);

#ifdef __cplusplus
}
#endif


#endif

