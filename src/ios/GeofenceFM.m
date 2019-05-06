#import "GeofenceFM.h"

#import <Cordova/CDVAvailability.h>



@implementation GeofenceFM

- (void)pluginInitialize {
}

- (void)init:(CDVInvokedUrlCommand*)command
{
    NSLog(@"init -> GeofenceFM");
    self.command = command;

    
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    center.delegate=self;
    [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound)
       completionHandler:^(BOOL granted, NSError * _Nullable error) {
	
           UNMutableNotificationContent *content = [UNMutableNotificationContent new];
           content.title = @"Alerta";
           content.body = @"Contenido de la alerta";
           content.sound = [UNNotificationSound defaultSound];
           UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1 repeats:NO];
           
           NSString *identifier = @"UYLLocalNotification";
           UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:identifier content:content trigger:trigger];
           
           [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
               if (error != nil) {
                   NSLog(@"ERROR: %@",error);
               }
           }];

    }];

    
    self.locationManager = [[CLLocationManager alloc] init];
    [self.locationManager setDelegate:self];

    self.fences = [[NSMutableDictionary alloc] init];

    if ([self.locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
        [self.locationManager requestAlwaysAuthorization];
    } else {
        [self.locationManager startUpdatingLocation];
        
        NSString* msg = [NSString stringWithFormat: @"OK"];
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:msg];
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
    NSLog(@"Callback");
    if (status == kCLAuthorizationStatusAuthorizedAlways) {
        NSLog(@"Authorized kCLAuthorizationStatusAuthorizedAlways");
        
        [self.locationManager startUpdatingLocation];
        
        NSString* msg = [NSString stringWithFormat: @"OK"];
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:msg];
        
        [self.commandDelegate sendPluginResult:result callbackId:self.command.callbackId];
        
    } else if (status == kCLAuthorizationStatusAuthorizedWhenInUse) {
        NSLog(@"Authorized kCLAuthorizationStatusAuthorizedWhenInUse");

        [self.locationManager startUpdatingLocation];
        
        NSString* msg = [NSString stringWithFormat: @"PERMISSION_PARTIAL"];
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:msg];
        
        [self.commandDelegate sendPluginResult:result callbackId:self.command.callbackId];
        
    } else if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
        NSLog(@"Denied");
        
        [self.locationManager startUpdatingLocation];
        
        NSString* msg = [NSString stringWithFormat: @"PERMISSION_DENIED"];
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:msg];
        
        [self.commandDelegate sendPluginResult:result callbackId:self.command.callbackId];
    }
}

- (void)addOrUpdateFence:(CDVInvokedUrlCommand*)command
{
    
    NSArray* array = [command.arguments objectAtIndex:0];
    
    for (id object in array) {
        
        NSMutableDictionary* options = object;
        double latitud = [[options objectForKey:@"latitud"] doubleValue];
        double longitud = [[options objectForKey:@"longitud"] doubleValue];
        double radius = [[options objectForKey:@"radius"] doubleValue];
        NSString* _id = [options objectForKey:@"id"];
        
        CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitud, longitud);
        CLRegion *bridge = [[CLCircularRegion alloc]initWithCenter:center radius:radius identifier:_id];
        
        [self.fences setValue:bridge forKey:_id];

        [self.locationManager startMonitoringForRegion:bridge];
    }
    
    NSString* msg = [NSString stringWithFormat: @"OK"];
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus:CDVCommandStatus_OK
                               messageAsString:msg];
    
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region {
    NSLog(@"hola desde enter region");
}


-(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region {
    
}

-(void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region {
    NSLog(@"Now monitoring for %@", region.identifier);
    [self.locationManager performSelector:@selector(requestStateForRegion:) withObject:region afterDelay:2];

}

- (void)locationManager:(CLLocationManager *)manager didDetermineState:(CLRegionState)state forRegion:(CLRegion *)region {
    
    if (state == CLRegionStateInside){
        
        [self enterGeofence:region];
        
    } else if (state == CLRegionStateOutside){
        
        [self exitGeofence:region];
        
    } else if (state == CLRegionStateUnknown){
        NSLog(@"Unknown state for geofence: %@", region);
        return;
    }
}

- (void)enterGeofence:(CLRegion *)region {
    NSLog(@"enterGeofence -> %@", region.identifier);
    
    NSArray *array = [region.identifier componentsSeparatedByString:@"|"];
    NSLog(@"%@",array);
    
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    content.title = @"Alerta";
    content.body = @"Entro al area";
    content.sound = [UNNotificationSound defaultSound];
    UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1 repeats:NO];
    
    NSString *identifier = @"UYLLocalNotification";
    UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:identifier content:content trigger:trigger];
    
    [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"ERROR: %@",error);
        }
    }];
    
}

- (void)exitGeofence:(CLRegion *)region {
    NSLog(@"exitGeofence -> %@", region.identifier);
    
    NSArray *array = [region.identifier componentsSeparatedByString:@"|"];
    NSLog(@"%@",array);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    // Update the app interface directly.
    
    // Play a sound.
    NSLog(@"prueba de notificacion : ");
    completionHandler(UNNotificationPresentationOptionSound);
    
    NSString *message = @"Some message...";
    
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];
    
    int duration = 1; // duration in seconds
    
    NSArray *misAlertas;
    
    misAlertas = [NSArray arrayWithObjects: @"aleta 1", @"alerta 2", @"alerta 3", @"alerta 4", nil];
    
    int count = [misAlertas count];
    int a =0+arc4random()%count;
    
    NSString *mensajeAlerta=[misAlertas objectAtIndex: a];
    NSLog (@"Elemento ramdom = %@", [misAlertas objectAtIndex: a]);
    
    [WPSAlertController presentOkayAlertWithTitle:@"alerta" message:mensajeAlerta];
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, duration * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [alert dismissViewControllerAnimated:YES completion:nil];
    });
}




@end
