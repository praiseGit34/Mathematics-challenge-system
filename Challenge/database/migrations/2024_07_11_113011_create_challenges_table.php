<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateChallengesTable extends Migration
{
    public function up()
    {
        Schema::create('challenges', function (Blueprint $table) {
            $table->id();
            $table->string('challengeNo');
            $table->string('challengeName');
            $table->dateTime('startDate');
            $table->dateTime('endDate');
            $table->integer('duration');
            $table->integer('numOfQuestions');
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('challenges');
    }
}
