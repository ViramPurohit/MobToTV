package shaadi.com.di.modules

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shaadi.com.BuildConfig
import shaadi.com.ShaadiApplication
import shaadi.com.api.ApiService
import shaadi.com.api.ShaadiRepository
import shaadi.com.db.ShaadiDb
import shaadi.com.db.ShaadiUserDao
import shaadi.com.di.ViewModelModule
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = [(ViewModelModule::class)])
class AppModule {

    @Provides
    @Singleton
    fun provideApp(app: Application): ShaadiApplication {
        return app as ShaadiApplication
    }

    @Provides
    @Singleton
    fun provideContext(app: Application): Context {
        return app.applicationContext
    }



    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    @Provides
    @Singleton
    fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(httpLoggingInterceptor).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder().client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BuildConfig.Shaadi_API)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }



    @Singleton
    @Provides
    fun provideDb(app: Application): ShaadiDb {
        return Room
            .databaseBuilder(app, ShaadiDb::class.java, "shaadi_db.db")
            .allowMainThreadQueries()   //Allows room to do operation on main thread
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideShaadiUserDao(db: ShaadiDb): ShaadiUserDao {
        return db.shaadiUserDao()
    }
}