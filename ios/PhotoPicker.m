#import "PhotoPicker.h"

#import "NSDictionary+SYSafeConvert.h"
#import <AssetsLibrary/AssetsLibrary.h>
#import <React/RCTUtils.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <math.h>


typedef void (^ ImageSuccessBlock)(UIImage * _Nullable image, HXPhotoModel * _Nullable model, NSDictionary * _Nullable info);

@interface PhotoPickerModule ()

@property (strong, nonatomic) HXPhotoManager *manager;
/**
 保存Promise的resolve block
 */
@property (nonatomic, copy) RCTPromiseResolveBlock resolveBlock;
/**
 保存Promise的reject block
 */
@property (nonatomic, copy) RCTPromiseRejectBlock rejectBlock;

@property (nonatomic, strong) NSDictionary *pickerOptions;

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
	self.resolveBlock = resolve;
	self.rejectBlock = reject;
	self.pickerOptions = options;
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
	_manager.configuration.hideOriginalBtn = YES;
	// 当原图按钮隐藏时选择原图
	_manager.configuration.requestOriginalImage = YES;
    
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
        NSInteger cropWidthRatio = [options sy_integerForKey:@"cropWidthRatio"];
        NSInteger cropHeightRatio = [options sy_integerForKey:@"cropHeightRatio"];
        if (cropWidthRatio <= 0 || cropHeightRatio <= 0) {
            _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_Original;
        } else {
            _manager.configuration.photoEditConfigur.customAspectRatio = CGSizeMake(cropWidthRatio, cropHeightRatio);
        }
        
        if ([options sy_boolForKey:@"customCropRatio"]) {
            _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_None;
        } else {
            _manager.configuration.photoEditConfigur.aspectRatio = HXPhotoEditAspectRatioType_Custom;
        }
    }
    
	// 不使用完成
    _manager.configuration.requestImageAfterFinishingSelection = NO;
	// 当选择原图时导出最高质量
	_manager.configuration.exportVideoURLForHighestQuality = YES;
	
    
    [_manager clearSelectedList];
	
	HXCustomNavigationController *nav = [[HXCustomNavigationController alloc] initWithManager:_manager delegate:self];
	UIViewController *rootViewController = RCTPresentedViewController();
	[rootViewController presentViewController:nav animated:YES completion:nil];
}

RCT_REMAP_METHOD(clean,
                 cleanResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    [fileManager removeItemAtPath: [NSString stringWithFormat:@"%@ImageCropPicker", NSTemporaryDirectory()] error:nil];
    
    resolve(nil);
}

/**
点击完成按钮

@param photoNavigationViewController self
@param allList 已选的所有列表(包含照片、视频)
@param photoList 已选的照片列表
@param videoList 已选的视频列表
@param original 是否原图
*/
- (void)photoNavigationViewController:(HXCustomNavigationController *)photoNavigationViewController didDoneAllList:(NSArray<HXPhotoModel *> *)allList photos:(NSArray<HXPhotoModel *> *)photoList videos:(NSArray<HXPhotoModel *> *)videoList original:(BOOL)original{
	BOOL isOriginal = original || _manager.configuration.requestOriginalImage;
	[SVProgressHUD setDefaultStyle:SVProgressHUDStyleDark];
	[SVProgressHUD showWithStatus:@"文件处理中，请稍后..."];
	NSMutableArray *files = [NSMutableArray array];
	[allList enumerateObjectsUsingBlock:^(HXPhotoModel*  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
		NSMutableDictionary *file  = [NSMutableDictionary dictionary];
		[obj getAssetURLWithVideoPresetName:isOriginal ? AVAssetExportPresetHighestQuality : AVAssetExportPresetMediumQuality success:^(NSURL * _Nullable url, HXPhotoModelMediaSubType mediaType, BOOL isNetwork, HXPhotoModel * _Nullable model) {
			if (obj.subType == HXPhotoModelMediaSubTypePhoto) {
				NSData *writeData = [NSData dataWithContentsOfURL:url];
				if ([self.pickerOptions sy_boolForKey:@"isCompress"]) {
					UIImage *img = [UIImage imageWithData:writeData];
					NSInteger compressQuality = [self.pickerOptions sy_integerForKey:@"compressQuality"];
					CGFloat quality = (CGFloat)compressQuality / 100;
					writeData = [self smartCompressImage:img minimumCompressSize:[self.pickerOptions sy_integerForKey:@"minimumCompressSize"] compressQuality:quality];
				}
				UIImage *image = [UIImage imageWithData:writeData];

				NSString *suffix = @"jpeg";
				if (UIImagePNGRepresentation(image)) {
					//返回为png图像。
					writeData = UIImagePNGRepresentation(image);
					suffix = @"png";
				}else {
					//返回为JPEG图像。
					writeData = UIImageJPEGRepresentation(image, 1);
					suffix = @"jpeg";
				}

				[self createDir];
				NSString *fileName = [[NSString hx_fileName] stringByAppendingString:[NSString stringWithFormat:@".%@",suffix]];
				NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:fileName];;

				[writeData writeToFile:filePath atomically:YES];
				url = [NSURL fileURLWithPath:filePath];
				model.imageURL = url;
				model.previewPhoto = image;
			}

			file[@"path"] = url.path;
			file[@"uri"] = url.absoluteString;

			NSDictionary *dictionary = [[NSFileManager defaultManager] attributesOfItemAtPath:[url path] error:nil];
			file[@"fileName"] = [[url path] lastPathComponent];
			
			file[@"size"] = [dictionary objectForKey:NSFileSize];
			file[@"duration"] = @(model.videoDuration * 1000);
			file[@"mime"] = [self getMimeType:url.path];
			
			BOOL isVideo = mediaType == HXPhotoModelMediaSubTypeVideo;
			BOOL isImage = mediaType == HXPhotoModelMediaSubTypePhoto;
			
			if (isVideo) {
				AVAsset *asset = [AVAsset assetWithURL:url];
				NSArray *tracks = [asset tracksWithMediaType:AVMediaTypeVideo];
				AVAssetTrack *videoTrack = tracks[0];
				CGSize videoSize = CGSizeApplyAffineTransform(videoTrack.naturalSize, videoTrack.preferredTransform);
				file[@"width"] = @(fabs(videoSize.width));
				file[@"height"] = @(fabs(videoSize.height));
			} else {
				file[@"width"] = @(model.previewPhoto.size.width);
				file[@"height"] = @(model.previewPhoto.size.height);
			}
			
			BOOL isCover = [self.pickerOptions sy_boolForKey:@"isCover"];
			if (isImage && isCover) {
				NSDictionary *cover = [self handleCoverImage:obj.previewPhoto compressQuality:100];
				file[@"coverFileName"] = cover[@"filename"];
				file[@"coverPath"] = cover[@"path"];
				file[@"coverUri"] = cover[@"uri"];
				file[@"coverMime"] = cover[@"mime"];
				file[@"coverSize"] = cover[@"size"];
			} else if (isVideo && isCover) {
				NSDictionary *cover = [self handleCoverImage:[self getLocationVideoPreViewImage:url] compressQuality:100];
				file[@"coverFileName"] = cover[@"filename"];
				file[@"coverPath"] = cover[@"path"];
				file[@"coverUri"] = cover[@"uri"];
				file[@"coverMime"] = cover[@"mime"];
				file[@"coverSize"] = cover[@"size"];
			}
			
			file[@"isVideo"] = @(isVideo);
			if ([self.pickerOptions sy_boolForKey:@"includeBase64"] && model.subType == HXPhotoModelMediaSubTypePhoto) {
				NSData *writeData = model.photoFormat == HXPhotoModelFormatPNG ? UIImagePNGRepresentation(model.previewPhoto) : UIImageJPEGRepresentation(model.previewPhoto, 1);
				file[@"data"] = [NSString stringWithFormat:@"%@", [writeData base64EncodedStringWithOptions:0]];
			}
			
			[files addObject:file];
			if ([files count] == [allList count]) {
				[SVProgressHUD dismiss];
				self.resolveBlock(files);
			}
		} failed:^(NSDictionary * _Nullable info, HXPhotoModel * _Nullable model) {
			self.rejectBlock(@"error", @"error", nil);
		}];
	}];
}

/**
点击取消

@param photoNavigationViewController self
*/
- (void)photoNavigationViewControllerDidCancel:(HXCustomNavigationController *)photoNavigationViewController {
	self.rejectBlock(@"cancel", @"cancel", nil);
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
	
	CGSize imageSize = image.size;//取出要压缩的image尺寸
	CGFloat width = imageSize.width;    //图片宽度
	CGFloat height = imageSize.height;  //图片高度
	
	CGFloat scale = height / width;
	width = fmin(width, 375);
	height = width * scale;
	
	UIGraphicsBeginImageContext(CGSizeMake(width, height));
	[image drawInRect:CGRectMake(0, 0, width, height)];
	UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
	UIGraphicsEndImageContext();
	
	NSData *cacheData = isPNG ? UIImagePNGRepresentation(newImage) : UIImageJPEGRepresentation(newImage, 1);
    
	NSData *writeData = isPNG ? UIImagePNGRepresentation(newImage) : UIImageJPEGRepresentation(newImage, cacheData.length > 100 ? compressQuality/100 : 1);
    [writeData writeToFile:filePath atomically:YES];
    
    NSMutableDictionary *photo = [NSMutableDictionary dictionary];
    
    photo[@"filename"] = filename;
    photo[@"uri"] = [[NSURL fileURLWithPath:filePath] absoluteString];
    photo[@"path"] = filePath;
    photo[@"mime"] = [self getMimeType:filePath];
	photo[@"size"] = @([writeData length]);
    
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

// 获取本地视频第一帧
- (UIImage*) getLocationVideoPreViewImage:(NSURL *)path
{
	AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:path options:nil];
	AVAssetImageGenerator *assetGen = [[AVAssetImageGenerator alloc] initWithAsset:asset];
	
	assetGen.appliesPreferredTrackTransform = YES;
	CMTime time = CMTimeMakeWithSeconds(0.0, 600);
	NSError *error = nil;
	CMTime actualTime;
	CGImageRef image = [assetGen copyCGImageAtTime:time actualTime:&actualTime error:&error];
	UIImage *videoImage = [[UIImage alloc] initWithCGImage:image];
	CGImageRelease(image);
	return videoImage;
}

/// 创建PhotoPickerModule缓存目录
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

- (NSData *)smartCompressImage:(UIImage *)sourceImage minimumCompressSize:(NSInteger)minimumCompressSize compressQuality:(CGFloat) compressQuality {
    //进行图像尺寸的压缩
    CGSize imageSize = sourceImage.size;//取出要压缩的image尺寸
    CGFloat width = imageSize.width;    //图片宽度
    CGFloat height = imageSize.height;  //图片高度
    //1.宽高大于1280(宽高比不按照2来算，按照1来算)
    if (width>1280||height>1280) {
        if (width>height) {
            CGFloat scale = height/width;
            width = 1280;
            height = width*scale;
        }else{
            CGFloat scale = width/height;
            height = 1280;
            width = height*scale;
        }
    //2.宽大于1280高小于1280
    }else if(width>1280||height<1280){
        CGFloat scale = height/width;
        width = 1280;
        height = width*scale;
    //3.宽小于1280高大于1280
    }else if(width<1280||height>1280){
        CGFloat scale = width/height;
        height = 1280;
        width = height*scale;
    //4.宽高都小于1280
    }else{
    }
    UIGraphicsBeginImageContext(CGSizeMake(width, height));
    [sourceImage drawInRect:CGRectMake(0,0,width,height)];
    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    //进行图像的画面质量压缩
    NSData *data=UIImageJPEGRepresentation(newImage, 1.0);
    if (data.length > minimumCompressSize*1024) {
        if (data.length > 1024*1024) {
            //1M以及以上
            data=UIImageJPEGRepresentation(newImage, compressQuality);
        }else if (data.length > 512*1024) {
            //0.5M-1M
            data=UIImageJPEGRepresentation(newImage, 0.6);
        }else if (data.length > 200*1024) {
            //0.25M-0.5M
            data=UIImageJPEGRepresentation(newImage, 0.9);
        }
    }
    return data;
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

