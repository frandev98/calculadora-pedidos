package com.francisco.calculadorapedidos.di

import android.content.Context
import com.francisco.calculadorapedidos.data.ClientRepository
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.data.OrderRepository
import com.francisco.calculadorapedidos.data.db.ClientDao
import com.francisco.calculadorapedidos.data.db.DraftDao
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
    fun provideOrderDao(database: FuxionDatabase): OrderDao = database.orderDao()

    @Provides
    @Singleton
    fun provideClientDao(database: FuxionDatabase): ClientDao = database.clientDao()

    @Provides
    @Singleton
    fun provideDraftDao(database: FuxionDatabase): DraftDao = database.draftDao()

    @Provides
    @Singleton
    fun provideOrderRepository(orderDao: OrderDao, draftDao: DraftDao): OrderRepository {
        return OrderRepository(orderDao, draftDao)
    }

    @Provides
    @Singleton
    fun provideClientRepository(clientDao: ClientDao): ClientRepository {
        return ClientRepository(clientDao)
    }

    @Provides
    @Singleton
    fun provideFuxionDataStore(@ApplicationContext context: Context): FuxionDataStore {
        return FuxionDataStore(context)
    }
}