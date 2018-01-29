
#import "RNReactNativeImageData.h"
#import "UIImage+ColorAtPixel.h"
#import <React/RCTImageLoader.h>

@implementation RNReactNativeImageData

@synthesize bridge = _bridge;

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(getSimpleGrayscalePixels:(NSString *)path
                    options:(NSDictionary *)options
                    findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                    rejecter:(RCTPromiseRejectBlock)reject)
{
    NSURL *imageUrl = [[NSURL alloc] initWithString:path];
    UIImage *image = [UIImage imageWithData:[NSData dataWithContentsOfURL:imageUrl]];

    if (image == nil) {
        NSError *error = [NSError errorWithDomain:@"com.mol42" code:500 userInfo:@{@"Error reason": @"Image could not be read"}];
        reject(@"no_events", @"There were no events", error);
        return;
    }

    NSInteger maxWidth = [RCTConvert NSInteger:options[@"maxWidth"]];
    NSInteger maxHeight = [RCTConvert NSInteger:options[@"maxHeight"]];
    CGSize newSize = CGSizeMake(maxWidth, maxHeight);
    UIImage *scaledImage = [self scaleImage:image toSize:newSize];

    for (int x = 0; x < maxWidth; x++) {
        CGPoint point = CGPointMake(x, 0);
        UIColor *pixelColor = [image colorAtPixel:point];
        NSString *hexString = hexStringForColor(pixelColor)
        NSLog(@"hexString");
        NSLog(hexString);
        /*
        for (int j = 0; j < maxHeight; j++) {
            
        }
        */
    }
    /*
    if (options[@"width"] && options[@"height"]) {
        NSInteger scaledWidth = [RCTConvert NSInteger:options[@"width"]];
        NSInteger scaledHeight = [RCTConvert NSInteger:options[@"height"]];
        float originalWidth = image.size.width;
        float originalHeight = image.size.height;
        
        x = x * (originalWidth / scaledWidth);
        y = y * (originalHeight / scaledHeight);
        
    }
    */
    //CGPoint point = CGPointMake(x, y);
    
    //UIColor *pixelColor = [image colorAtPixel:point];
    //callback(@[[NSNull null], hexStringForColor(pixelColor)]);
    resolve(NULL);
}

NSString * hexStringForColor( UIColor* color ) {
    const CGFloat *components = CGColorGetComponents(color.CGColor);
    CGFloat r = components[0];
    CGFloat g = components[1];
    CGFloat b = components[2];
    NSString *hexString=[NSString stringWithFormat:@"#%02X%02X%02X", (int)(r * 255), (int)(g * 255), (int)(b * 255)];

    return hexString;
}

- (UIImage *)scaleImage:(UIImage *)originalImage toSize:(CGSize)size
{       
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(NULL, size.width, size.height, 8, 0, colorSpace, kCGImageAlphaPremultipliedLast);
    CGContextClearRect(context, CGRectMake(0, 0, size.width, size.height));
    
    if (originalImage.imageOrientation == UIImageOrientationRight) {
        CGContextRotateCTM(context, -M_PI_2);
        CGContextTranslateCTM(context, -size.height, 0.0f);
        CGContextDrawImage(context, CGRectMake(0, 0, size.height, size.width), originalImage.CGImage);
    } else {
        CGContextDrawImage(context, CGRectMake(0, 0, size.width, size.height), originalImage.CGImage);
    }
        
    CGImageRef scaledImage = CGBitmapContextCreateImage(context);
    CGColorSpaceRelease(colorSpace);
    CGContextRelease(context);
    
    UIImage *image = [UIImage imageWithCGImage:scaledImage];
    CGImageRelease(scaledImage);
    
    return image;
}

@end
  