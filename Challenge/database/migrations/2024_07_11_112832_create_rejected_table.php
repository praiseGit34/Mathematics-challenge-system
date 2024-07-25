<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRejectedTable extends Migration
{
    public function up()
    {
        Schema::create('rejected', function (Blueprint $table) {
            $table->id();
            $table->string('username',50)->unique();
            $table->string('firstName');
            $table->string('lastName');
            $table->string('email',191)->unique();
            $table->date('dateOfBirth');
            $table->string('schoolRegNo',50);
            $table->string('imagePath');
            $table->string('password')->nullable();
            $table->timestamps();

            $table->foreign('schoolRegNo')->references('schoolRegNo')->on('schools')->onDelete('cascade');
        });
    }

    public function down()
    {
        Schema::dropIfExists('rejected');
    }
}
