<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAnswersTable extends Migration
{
    public function up()
    {
        Schema::create('answers', function (Blueprint $table) {
            $table->id();
            $table->string('questionId', 191);  // Limit the length to 191 characters
            $table->text('answer');
            $table->integer('mark');
            $table->unsignedBigInteger('challengeNo');
            $table->timestamps();
            
            //Add foreign key constraint if you have a challenges table
            $table->foreign('challengeNo')->references('id')->on('challenges')->onDelete('cascade');
            $table->foreign('questionId')->references('questionId')->on('questions')->onDelete('cascade');
        });
    }

    public function down()
    {
        Schema::dropIfExists('answers');
    }
}