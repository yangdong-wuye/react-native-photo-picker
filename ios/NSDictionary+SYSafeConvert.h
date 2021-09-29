//
//  NSDictionary+SYSafeConvert.h
//  tafengkejipro
//
//  Created by lollipop on 2021/9/26.
//

#import <Foundation/Foundation.h>

@interface NSMutableDictionary (SYSafeConvert)

- (void)sy_setObject:(id)value forKey:(NSString *)key;

- (void)sy_setInteger:(NSInteger)value forKey:(NSString *)key;

- (void)sy_setBool:(BOOL)value forKey:(NSString *)key;

@end

@interface NSDictionary (SYSafeConvert)

- (NSString *)sy_stringForKey:(NSString *)key;

- (BOOL)sy_boolForKey:(NSString *)key;

- (NSInteger)sy_integerForKey:(NSString *)key;

@end

