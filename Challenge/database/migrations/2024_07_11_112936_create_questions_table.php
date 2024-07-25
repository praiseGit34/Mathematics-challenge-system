<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQuestionsTable extends Migration
{
    public function up()
    {
        Schema::create('questions', function (Blueprint $table) {
            $table->string('id');
            $table->string('questionId');
            $table->text('question');
            $table->unsignedBigInteger('challengeNo')->nullable();
            $table->timestamps();
       
            // Add foreign key constraint if you have a challenges table
             $table->foreign('challengeNo')->references('id')->on('challenges')->onDelete('cascade');
        });
    }

    public function down()
    {
        Schema::dropIfExists('questions');
    }
}