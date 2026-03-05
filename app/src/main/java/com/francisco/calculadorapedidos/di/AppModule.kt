package com.francisco.calculadorapedidos.di

import android.content.Context
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.data.db.FuxionDatabase
import com.francisco.calculadorapedidos.data.db.OrderDao
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
    fun provideFuxionDatabase(@ApplicationContext context: Context): FuxionDatabase {
        return FuxionDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideOrderDao(database: FuxionDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    @Singleton
    fun provideOrderRepository(@ApplicationContext context: Context): OrderRepository {
        return OrderRepository(context)
    }
}