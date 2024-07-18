<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateChallengeTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('challenge', function (Blueprint $table) {
            $table->id();
            $table->string('challengeNo');
            $table->string('challengeName');
            $table->time('duration');
            $table->integer('noOfQuestions');
            $table->integer('overallMark');
            $table->date('openDate');
            $table->date('closeDate');
            $table->unsignedBigInteger('question_bank_id');
            $table->timestamps();
        
            $table->foreign('question_bank_id')->references('id')->on('question_banks');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('challenge');
    }
}
