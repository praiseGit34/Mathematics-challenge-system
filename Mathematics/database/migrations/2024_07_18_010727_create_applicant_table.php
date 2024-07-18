<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateApplicantTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('applicant', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('schoolRegNo');
            $table->string('emailAddress',191)->unique();
            $table->string('userName',191)->unique();
            $table->string('imagePath');
            $table->string('firstName');
            $table->string('lastName');
            $table->string('password');
            $table->date('dateOfBirth');
            $table->timestamps();
        
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
        Schema::dropIfExists('applicant');
    }
}
