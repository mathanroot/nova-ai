package com.example.nova.di

import android.content.Context
import com.example.nova.actions.AlarmAction
import com.example.nova.actions.AppLaunchAction
import com.example.nova.actions.CalendarAction
import com.example.nova.actions.CallAction
import com.example.nova.actions.DeviceControlAction
import com.example.nova.actions.NavigationAction
import com.example.nova.actions.ReminderAction
import com.example.nova.actions.SmsAction
import com.example.nova.actions.TimerAction
import com.example.nova.permissions.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideAlarmAction(@ApplicationContext context: Context): AlarmAction {
        return AlarmAction(context)
    }

    @Provides
    @Singleton
    fun provideTimerAction(@ApplicationContext context: Context): TimerAction {
        return TimerAction(context)
    }

    @Provides
    @Singleton
    fun provideCallAction(@ApplicationContext context: Context): CallAction {
        return CallAction(context)
    }

    @Provides
    @Singleton
    fun provideSmsAction(@ApplicationContext context: Context, callAction: CallAction): SmsAction {
        return SmsAction(context, callAction)
    }

    @Provides
    @Singleton
    fun provideAppLaunchAction(@ApplicationContext context: Context): AppLaunchAction {
        return AppLaunchAction(context)
    }

    @Provides
    @Singleton
    fun provideNavigationAction(@ApplicationContext context: Context): NavigationAction {
        return NavigationAction(context)
    }

    @Provides
    @Singleton
    fun provideReminderAction(@ApplicationContext context: Context): ReminderAction {
        return ReminderAction(context)
    }

    @Provides
    @Singleton
    fun provideCalendarAction(@ApplicationContext context: Context): CalendarAction {
        return CalendarAction(context)
    }

    @Provides
    @Singleton
    fun provideDeviceControlAction(@ApplicationContext context: Context): DeviceControlAction {
        return DeviceControlAction(context)
    }
}
