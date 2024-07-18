<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAttemptTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('attempt', function (Blueprint $table) {
            $table->id();
            $table->timestamp('startTime');
            $table->timestamp('endTime')->nullable();
            $table->unsignedBigInteger('participantId');
            $table->unsignedBigInteger('challengeId');
            $table->integer('score')->nullable();
            $table->decimal('percentageMark', 5, 2)->nullable();
            $table->timestamps();
        
            $table->foreign('participantId')->references('id')->on('participant');
            $table->foreign('challengeId')->references('id')->on('challenge');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('attempt');
    }
}
