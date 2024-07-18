<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateParticipantTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('participant', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('applicant_id');
            $table->string('firstName');
            $table->string('lastName');
            $table->string('emailAddress',191)->unique();
            $table->date('dateOfBirth');
            $table->unsignedBigInteger('schoolRegNo');
            $table->string('userName',191)->unique();
            $table->string('imagePath');
            $table->string('password');
            $table->timestamps();
        
            $table->foreign('applicant_id')->references('id')->on('applicant');
            $table->foreign('schoolRegNo')->references('id')->on('school');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('participant');
    }
}
