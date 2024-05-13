package com.kylecorry.trail_sense_comms.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SampleDao {
    @Query("SELECT * FROM sample")
    fun getAll(): LiveData<List<SampleEntity>>

    @Query("SELECT * FROM sample WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): SampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dream: SampleEntity): Long

    @Delete
    suspend fun delete(dream: SampleEntity)

    @Update
    suspend fun update(dream: SampleEntity)
}