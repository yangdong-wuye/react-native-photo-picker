#import "PhotoPicker.h"

#import "NSDictionary+SYSafeConvert.h"
#import <AssetsLibrary/AssetsLibrary.h>
#import <React/RCTUtils.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import "HXPhotoPicker.h"

@interface PhotoPickerModule ()

@property (strong, nonatomic) HXPhotoManager *manager;

@end

@implementation PhotoPickerModule

- (instancetype)init {
    self = [super init];
    if (self) {
        _manager = [[HXPhotoManager alloc] initWithType:HXPhotoManagerSelectedTypePhotoAndVideo];
    }
    return self;
}


RCT_EXPORT_MODULE()


RCT_REMAP_METHOD(openPicker,
                 options:(NSDictionary *)options
                 showImagePickerResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    NSInteger type = [options sy_integerForKey:@"type"];
    _manager.type = type;
    _manager.configuration.cameraPhotoJumpEdit = YES;
    _manager.configuration.creationDateSort = YES;
    _manager.configuration.reverseDate = NO;
    _manager.configuration.videoCodecKey = @"avc1";
    _manager.configuration.maxNum = [options sy_integerForKey:@"maxNum"];
    NSInteger videoMaxNum = type == HXPhotoManagerSelectedTypePhoto ? 0 : [options sy_integerForKey:@"videoMaxNum"];
    _manager.configuration.photoMaxNum = [options sy_integerForKey:@"maxNum"] - videoMaxNum;
    _manager.configuration.videoMaxNum = videoMaxNum;
    _manager.configuration.openCamera = [options sy_boolForKey:@"openCamera"];
    _manager.configuration.lookGifPhoto = [options sy_boolForKey:@"lookGifPhoto"];
    _manager.configuration.lookLivePhoto = [options sy_boolForKey:@"lookLivePhoto"];
    _manager.configuration.selectTogether = [options sy_boolForKey:@"selectTogether"];
    
    NSInteger maxFileSize = [options sy_integerForKey:@"maxFileSize"];
    _manager.configuration.limitPhotoSize = maxFileSize;
    _manager.configuration.limitVideoSize = maxFileSize;
    _manager.configuration.selectPhotoLimitSize = maxFileSize != 0;
    _manager.configuration.selectVideoLimitSize = maxFileSize != 0;
    
    _manager.configuration.videoMaximumDuration = [options sy_integerForKey:@"videoMaximumDuration"];
    _manager.configuration.videoMinimumDuration = [options sy_integerForKey:@"videoMinimumDuration"];
    _manager.configuration.deleteTemporaryPhoto = [options sy_boolForKey:@"deleteTemporaryPhoto"];
    _manager.configuration.saveSystemAblum = [options sy_boolForKey:@"saveSystemAlbum"];
    _manager.configuration.videoMaximumSelectDuration = [options sy_integerForKey:@"videoMaximumSelectDuration"];
    _manager.configuration.videoMinimumSelectDuration = [options sy_integerForKey:@"videoMinimumSelectDuration"];
    _manager.configuration.videoCanEdit = [options sy_boolForKey:@"videoCanEdit"];
    // 是否是单选模式
    _manager.configuration.singleSelected = [options sy_boolForKey:@"singleSelected"];
    _manager.configuration.singleJumpEdit = [options sy_boolForKey:@"singleJumpEdit"];
    
    // 裁剪设置
    _manager.configuration.photoCanEdit = [options sy_boolForKey:@"photoCanEdit"];
    // 拍照完成后是否进入编辑页面
    _manager.configuration.cameraPhotoJumpEdit = [options sy_boolForKey:@"photoCanEdit"];
    // 只要裁剪功能
    _manager.configuration.photoEditConfigur.onlyCliping = YES;
    
    
    if ([options sy_boolForKey:@"isRoundCliping"]) {
        _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_1x1;
        _manager.configuration.photoEditConfigur.isRoundCliping = true;
    } else {
        _manager.configuration.photoEditConfigur.isRoundCliping = false;
        _manager.configuration.photoEditConfigur.customAspectRatio = CGSizeMake([options sy_integerForKey:@"cropWidthRatio"], [options sy_integerForKey:@"cropHeightRatio"]);
        if ([options sy_boolForKey:@"customCropRatio"]) {
            _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_None;
        } else {
            _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_Custom;
            
        }
    }
    
    _manager.configuration.requestImageAfterFinishingSelection = YES;
    
    [_manager clearSelectedList];
    
    [[self topViewController] hx_presentSelectPhotoControllerWithManager: _manager didDone:^(NSArray<HXPhotoModel *> *allList, NSArray<HXPhotoModel *> *photoList, NSArray<HXPhotoModel *> *videoList, BOOL isOriginal, UIViewController *viewController, HXPhotoManager *manager) {
        NSMutableArray *files = [NSMutableArray array];
        [allList enumerateObjectsUsingBlock:^(HXPhotoModel*  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            NSMutableDictionary *file  = [NSMutableDictionary dictionary];
            [obj getAssetURLWithVideoPresetName:nil success:^(NSURL * _Nullable url, HXPhotoModelMediaSubType mediaType, BOOL isNetwork, HXPhotoModel * _Nullable model) {
                file[@"path"] = url.path;
                file[@"uri"] = url.absoluteString;
                if (obj.type == HXPhotoModelMediaTypeCameraPhoto || obj.type == HXPhotoModelMediaTypeCameraVideo) {
                    NSDictionary *dictionary = [[NSFileManager defaultManager] attributesOfItemAtPath:[url path] error:nil];
                    file[@"fileName"] = [[url path] lastPathComponent];
                    file[@"width"] = @(model.previewPhoto.size.width);
                    file[@"height"] = @(model.previewPhoto.size.height);
                    file[@"size"] = [dictionary objectForKey:NSFileSize];
                } else {
                    file[@"fileName"] = [model.asset valueForKey:@"filename"];
                    file[@"width"] = @(model.asset.pixelWidth);
                    file[@"height"] = @(model.asset.pixelHeight);
                    file[@"size"] = @(model.assetByte);
                }
                file[@"duration"] = @(model.videoDuration * 1000);
                file[@"mime"] = [self getMimeType:url.path];
                file[@"isVideo"] = @(mediaType == HXPhotoModelMediaSubTypeVideo);
                if ([options sy_boolForKey:@"includeBase64"] && model.subType == HXPhotoModelMediaSubTypePhoto) {
                    NSData *writeData = model.photoFormat == HXPhotoModelFormatPNG ? UIImagePNGRepresentation(model.previewPhoto) : UIImageJPEGRepresentation(model.previewPhoto, 1);
                    file[@"data"] = [NSString stringWithFormat:@"%@", [writeData base64EncodedStringWithOptions:0]];
                }
                if (mediaType == HXPhotoModelMediaSubTypeVideo) {
                    NSDictionary *cover = [self handleCoverImage:model.previewPhoto compressQuality:80];
                    file[@"coverFileName"] = cover[@"filename"];
                    file[@"coverPath"] = cover[@"path"];
                    file[@"coverUri"] = cover[@"uri"];
                    file[@"coverMime"] = cover[@"mime"];
                }
                [files addObject:file];
                if ([files count] == [allList count]) {
                    if (resolve) {
                        resolve(files);
                    }
                }
            } failed:^(NSDictionary * _Nullable info, HXPhotoModel * _Nullable model) {
                reject(@"error", @"error", nil);
            }];
        }];
    } cancel:^(UIViewController *viewController, HXPhotoManager *manager) {
        reject(@"cancel", @"cancel", nil);
    }];
}

RCT_REMAP_METHOD(clean,
                 cleanResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    [fileManager removeItemAtPath: [NSString stringWithFormat:@"%@ImageCropPicker", NSTemporaryDirectory()] error:nil];
    
    resolve(nil);
}

/// 处理裁剪图片数据
- (NSDictionary *)handleCoverImage:(UIImage *)image compressQuality:(CGFloat)compressQuality {
    [self createDir];
    
    NSString *filename = [NSString stringWithFormat:@"%@%@", [[NSUUID UUID] UUIDString], @".png"];
    NSString *fileExtension = [filename pathExtension];
    NSMutableString *filePath = [NSMutableString string];
    BOOL isPNG = [fileExtension hasSuffix:@"PNG"] || [fileExtension hasSuffix:@"png"];
    
    if (isPNG) {
        [filePath appendString:[NSString stringWithFormat:@"%@PhotoPickerModule/%@", NSTemporaryDirectory(), filename]];
    } else {
        [filePath appendString:[NSString stringWithFormat:@"%@PhotoPickerModule/%@.jpg", NSTemporaryDirectory(), [filename stringByDeletingPathExtension]]];
    }
    
    NSData *writeData = isPNG ? UIImagePNGRepresentation(image) : UIImageJPEGRepresentation(image, compressQuality/100);
    [writeData writeToFile:filePath atomically:YES];
    
    NSMutableDictionary *photo = [NSMutableDictionary dictionary];
    
    photo[@"filename"] = filename;
    photo[@"uri"] = [[NSURL fileURLWithPath:filePath] absoluteString];
    photo[@"path"] = filePath;
    photo[@"mime"] = [self getMimeType:filePath];
    
    return photo;
}

/// 获取文件的 mime type
- (NSString *)getMimeType:(NSString *) path{
    NSURL *url = [NSURL fileURLWithPath:path];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    NSHTTPURLResponse *response = nil;
    [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:nil];
    return response.MIMEType;
}

/// 创建SyanImageCaches缓存目录
- (BOOL)createDir {
    NSString * path = [NSString stringWithFormat:@"%@PhotoPickerModule", NSTemporaryDirectory()];;
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL isDir;
    if(![fileManager fileExistsAtPath:path isDirectory:&isDir]) {
        //先判断目录是否存在，不存在才创建
        BOOL res = [fileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:nil];
        return res;
    } else {
        return NO;
    };
}


+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (UIViewController *)topViewController {
    UIViewController *rootViewController = RCTPresentedViewController();
    return rootViewController;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

@end

