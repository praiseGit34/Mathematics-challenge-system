<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAttemptQuestionTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('attemptQuestion', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('attemptId');
            $table->unsignedBigInteger('questionId');
            $table->integer('score');
            $table->text('givenAnswer');
            $table->timestamps();
        
            $table->foreign('attemptId')->references('id')->on('attempt');
            $table->foreign('questionId')->references('id')->on('question');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('attemptQuestion');
    }
}
